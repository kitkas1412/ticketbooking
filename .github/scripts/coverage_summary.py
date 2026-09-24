#!/usr/bin/env python3
"""In bảng độ phủ test (Markdown) từ các báo cáo JaCoCo của từng module.

Dùng: coverage_summary.py <jacoco.xml>...
"""
import sys
import xml.etree.ElementTree as ET

TYPES = ("LINE", "BRANCH")


def counters(report):
    """Lấy counter tổng của module (các <counter> con trực tiếp của <report>)."""
    result = {t: (0, 0) for t in TYPES}
    for counter in report.findall("counter"):
        if counter.get("type") in TYPES:
            result[counter.get("type")] = (int(counter.get("covered")), int(counter.get("missed")))
    return result


def cell(covered, missed):
    total = covered + missed
    if total == 0:
        return "—"
    return f"{covered / total:.1%} ({covered}/{total})"


def main(paths):
    rows = []
    totals = {t: [0, 0] for t in TYPES}
    for path in paths:
        report = ET.parse(path).getroot()
        module = counters(report)
        rows.append((report.get("name"), module))
        for t in TYPES:
            totals[t][0] += module[t][0]
            totals[t][1] += module[t][1]

    print("## 🧪 Test coverage\n")
    print("| Module | Line | Branch |")
    print("|---|---:|---:|")
    for name, module in sorted(rows):
        print(f"| {name} | {cell(*module['LINE'])} | {cell(*module['BRANCH'])} |")
    print(f"| **Total** | **{cell(*totals['LINE'])}** | **{cell(*totals['BRANCH'])}** |")


if __name__ == "__main__":
    main(sys.argv[1:])
