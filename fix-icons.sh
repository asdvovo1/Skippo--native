#!/usr/bin/env bash
# fix-icons.sh
# يحل خطأ Gradle: "Resource and asset merger: Duplicate resources"
#
# السبب: في أندرويد اسم الـ resource ما بياخدش الامتداد في الحسبان، فلو نفس الاسم
# موجود بامتدادين في نفس فولدر الـ qualifier (مثلاً mipmap-xxhdpi/ic_launcher_foreground.png
# و mipmap-xxhdpi/ic_launcher_foreground.webp) يبقى عندك resource معرّف مرتين.
#
# السكربت بيلاقي كل التعارضات دي في res/ ويسيب نسخة واحدة بس (الافتراضي: .webp)
# ويحذف الباقي. لو المشروع git بيستخدم git rm علشان الحذف يتسجّل.
#
# الاستخدام (من جوه جذر المشروع):
#   bash fix-icons.sh                 # عرض بس، من غير أي حذف (dry-run)
#   bash fix-icons.sh --apply         # تنفيذ الحذف
#   bash fix-icons.sh --apply --keep png     # يسيب الـ png ويحذف الـ webp
#   bash fix-icons.sh --apply --res path/to/res

set -uo pipefail

RES_DIR="app/src/main/res"
KEEP="webp"
APPLY=0

while [ $# -gt 0 ]; do
  case "$1" in
    --apply) APPLY=1; shift ;;
    --keep)  KEEP="${2:?--keep محتاج قيمة}"; shift 2 ;;
    --res)   RES_DIR="${2:?--res محتاج قيمة}"; shift 2 ;;
    -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
    *) echo "خيار غير معروف: $1" >&2; exit 2 ;;
  esac
done

KEEP="$(printf '%s' "$KEEP" | tr '[:upper:]' '[:lower:]' | sed 's/^\.//')"

if [ ! -d "$RES_DIR" ]; then
  echo "✗ مش لاقي فولدر الـ res: $RES_DIR" >&2
  echo "  شغّل السكربت من جوه جذر المشروع، أو حدّد المسار: --res <path>" >&2
  exit 1
fi

IS_GIT=0
git -C "$RES_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1 && IS_GIT=1

echo "res   : $RES_DIR"
echo "keep  : .$KEEP"
if [ "$APPLY" -eq 1 ]; then echo "mode  : APPLY (هيحذف فعلاً)"; else echo "mode  : DRY-RUN (عرض بس، مفيش حذف)"; fi
if [ "$IS_GIT" -eq 1 ]; then echo "git   : أيوه (هيستخدم git rm للملفات المتتبّعة)"; else echo "git   : لا"; fi
echo

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

# كل ملفات الصور: "فولدر/اسم_بدون_امتداد <TAB> المسار الكامل"
find "$RES_DIR" -type f \
  \( -iname '*.png' -o -iname '*.webp' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.gif' \) \
  -print0 2>/dev/null \
| while IFS= read -r -d '' f; do
    d="$(dirname "$f")"; b="$(basename "$f")"; n="${b%.*}"
    printf '%s\t%s\n' "$d/$n" "$f"
  done | sort > "$tmp"

keys="$(cut -f1 "$tmp" | uniq -d)"

if [ -z "$keys" ]; then
  echo "✓ مفيش أي تعارض في أسماء الـ resources. مفيش حاجة تتعمل."
  echo
  echo "لو البيلد لسه بيفشل، جرّب:  ./gradlew clean assembleDebug"
  exit 0
fi

conflicts=0
removed=0
skipped=0

while IFS= read -r key; do
  [ -n "$key" ] || continue
  conflicts=$((conflicts + 1))

  files="$(awk -F'\t' -v k="$key" '$1 == k { print $2 }' "$tmp")"

  keeper=""
  while IFS= read -r f; do
    ext="$(printf '%s' "${f##*.}" | tr '[:upper:]' '[:lower:]')"
    if [ "$ext" = "$KEEP" ]; then keeper="$f"; break; fi
  done <<EOF
$files
EOF

  # لو الامتداد المطلوب مش موجود في المجموعة، سيب أكبر ملف واحذف الباقي
  if [ -z "$keeper" ]; then
    keeper="$(printf '%s\n' "$files" | while IFS= read -r f; do [ -n "$f" ] && printf '%s\t%s\n' "$(wc -c < "$f")" "$f"; done | sort -rn | head -1 | cut -f2)"
    echo "⚠ $key : مفيش نسخة .$KEEP — هسيب الأكبر حجمًا"
  fi

  echo "• $key"
  echo "    keep   -> $(basename "$keeper")"

  while IFS= read -r f; do
    [ -n "$f" ] || continue
    [ "$f" = "$keeper" ] && continue
    echo "    delete -> $(basename "$f")"
    if [ "$APPLY" -eq 1 ]; then
      if [ "$IS_GIT" -eq 1 ] && git ls-files --error-unmatch "$f" >/dev/null 2>&1; then
        git rm -q -f "$f" && removed=$((removed + 1)) || skipped=$((skipped + 1))
      else
        rm -f "$f" && removed=$((removed + 1)) || skipped=$((skipped + 1))
      fi
    fi
  done <<EOF
$files
EOF
done <<EOF
$keys
EOF

echo
echo "تعارضات: $conflicts"

if [ "$APPLY" -eq 1 ]; then
  echo "اتحذف : $removed ملف"
  [ "$skipped" -gt 0 ] && echo "فشل  : $skipped ملف"
  echo
  echo "خلصنا. شغّل:  ./gradlew clean assembleDebug"
else
  echo
  echo "ده كان عرض بس. للتنفيذ:  bash fix-icons.sh --apply"
fi
