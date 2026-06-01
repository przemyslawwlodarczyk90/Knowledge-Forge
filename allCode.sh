find . -type f \
  \( -name "*.java" \
  -o -name "*.kt" \
  -o -name "*.xml" \
  -o -name "*.html" \
  -o -name "*.htm" \
  -o -name "*.css" \
  -o -name "*.js" \
  -o -name "*.ts" \
  -o -name "*.json" \
  -o -name "*.yml" \
  -o -name "*.yaml" \
  -o -name "*.properties" \
  -o -name "*.md" \
  -o -name "*.sql" \
  -o -name "Dockerfile" \
  -o -name "docker-compose*.yml" \
  -o -name "pom.xml" \
  -o -name "build.gradle*" \
  -o -name "*.sh" \
  \) \
  -not -path "*/target/*" \
  -not -path "*/build/*" \
  -not -path "*/.git/*" \
  -not -path "*/node_modules/*" \
  -not -path "*/dist/*" \
  -print0 \
| sort -z \
| while IFS= read -r -d '' f; do
    printf "\n\n===== %s =====\n\n" "$f"
    cat "$f"
  done > all-project-code.txt
