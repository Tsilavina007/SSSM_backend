import glob
import re

from ruamel.yaml import YAML

y = YAML(typ="safe")
files = sorted(glob.glob(r"D:\Documents D\Tsong\SSSM\back\sssm_backend\doc\api\11_inventory\*.yaml"))
bad = 0
total_ops = 0
for f in files:
    raw = open(f, encoding="utf-8").read()
    try:
        d = y.load(raw)
    except Exception as e:
        bad += 1
        print("FAIL parse", f, "-", str(e).replace("\n", " ")[:140])
        continue
    errs = []
    for kind, name in set(re.findall(r"#/components/([A-Za-z]+)/([A-Za-z]+)", raw)):
        if name not in d.get("components", {}).get(kind, {}):
            errs.append("dangling $ref: " + kind + "/" + name)
    declared = {t["name"] for t in d.get("tags", [])}
    used = set()
    ops = 0
    for p in d["paths"].values():
        for m, op in p.items():
            if isinstance(op, dict) and m in ("get", "post", "put", "patch", "delete"):
                ops += 1
                used |= set(op.get("tags", []))
                if "responses" not in op:
                    errs.append("operation without responses: " + m)
    total_ops += ops
    for t in used - declared:
        errs.append("undeclared tag: " + t)
    if errs:
        bad += 1
        print("FAIL", f, "->", "; ".join(errs))
    else:
        print("OK  ", f.split("\\")[-1], "-", len(d["paths"]), "paths,", ops, "operations")
print("---")
print("RESULT:", bad, "problem(s) on", len(files), "files,", total_ops, "operations total")