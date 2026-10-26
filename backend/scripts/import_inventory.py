
import csv
from decimal import Decimal, InvalidOperation
from pathlib import Path


def parse_count(value):
    if not value:
        raise ValueError("missing count")

    try:
        number = Decimal(value)
    except InvalidOperation:
        raise ValueError(f"invalid count: {value!r}") from None

    if not number.is_finite() or number < 0:
        raise ValueError(f"invalid count: {value!r}")

    if number != number.to_integral_value():
        raise ValueError(f"count must be a whole number: {value!r}")

    if number > 2_147_483_647:
        raise ValueError("count exceeds PostgreSQL INTEGER range")

    return int(number)


def sql_text(value):
    return "'" + value.replace("'", "''") + "'"


def main():
    backend_dir = Path(__file__).resolve().parents[1]
    source = backend_dir / "data" / "inventory_snapshot.csv"
    output = backend_dir / "sql" / "import_snapshot.sql"

    items = []
    errors = []
    seen = set()
    excluded_categories = {"Fasteners & Hardware", "Soldering & Tools"}

    with source.open(encoding="utf-8-sig", newline="") as file:
        reader = csv.reader(file)

        for line_number, row in enumerate(reader, start=1):
            cells = [cell.strip() for cell in row]

            if not any(cells):
                continue

            # Skip section titles and repeated column headings.
            if cells[0] and not any(cells[1:]):
                continue

            if cells[:2] == ["Category", "Item Name"]:
                continue

            if len(cells) != 7:
                errors.append(
                    f"Line {line_number}: expected 7 columns, got {len(cells)}"
                )
                continue

            category, name, front, back, checked_out, available, total = cells

            if category in excluded_categories:
                continue

            try:
                if not category or not name:
                    raise ValueError("missing category or item name")

                front = parse_count(front)
                back = parse_count(back)
                checked_out = parse_count(checked_out)
                available = parse_count(available)
                total = parse_count(total)

                if front + back != total:
                    raise ValueError("front + back does not equal total")

                if total - checked_out != available:
                    raise ValueError("total - checked out does not equal available")

                # The snapshot doesn't record where checked-out items came from.
                # Only import checkouts that can be attributed to Front Stock.
                if checked_out > 0 and back > 0:
                    raise ValueError("checkout source location needs manual review")

                front_available = front - checked_out
                back_available = back

                if front_available < 0:
                    raise ValueError("checked out exceeds Front Stock")

                key = (category.casefold(), name.casefold())

                if key in seen:
                    raise ValueError("duplicate category/item pair")

                seen.add(key)

                items.append({
                    "category": category,
                    "name": name,
                    "front": front_available,
                    "back": back_available,
                })

            except ValueError as error:
                errors.append(f"Line {line_number}: {name}: {error}")

    if errors:
        raise SystemExit("\n".join(errors))

    for item in items:
        print(
            f"{item['category']} | {item['name']} | "
            f"front: {item['front']} | back: {item['back']}"
        )

    print(f"\nValidated {len(items)} components.")

    statements = [
        r"\set ON_ERROR_STOP on",
        "BEGIN;",
        "SET LOCAL standard_conforming_strings = on;",
        """
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM components)
       OR EXISTS (SELECT 1 FROM inventory)
       OR EXISTS (SELECT 1 FROM categories)
       OR EXISTS (SELECT 1 FROM locations)
    THEN
        RAISE EXCEPTION
            'Import requires empty components, inventory, categories, and locations tables';
    END IF;
END;
$$;
""",
        """
INSERT INTO locations (name)
VALUES ('Front Stock'), ('Backstock');
""",
    ]

    for category in sorted({item["category"] for item in items}):
        statements.append(
            f"INSERT INTO categories (name) VALUES ({sql_text(category)});"
        )

    for item in items:
        name = sql_text(item["name"])
        category = sql_text(item["category"])
        front = item["front"]
        back = item["back"]

        statements.append(f"""
WITH new_component AS (
    INSERT INTO components (name, category_id)
    SELECT {name}, id
    FROM categories
    WHERE name = {category}
      AND parent_id IS NULL
    RETURNING id
)
INSERT INTO inventory (component_id, location_id, quantity)
SELECT c.id, l.id, counts.quantity
FROM new_component AS c
CROSS JOIN (
    VALUES ('Front Stock', {front}), ('Backstock', {back})
) AS counts(location_name, quantity)
JOIN locations AS l
    ON l.name = counts.location_name
   AND l.parent_id IS NULL;
""")

    statements.append("COMMIT;")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(statements) + "\n", encoding="utf-8")

    print(f"Generated SQL for {len(items)} components:")
    print(output)


if __name__ == "__main__":
    main()
