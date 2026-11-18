# Print the changelog section for one library release.
# Sections are headed "## <library> <version>"; stop at the next top-level
# section and skip blank lines before the first entry.
$0 ~ "^## " lib " " v "([[:space:]]|$)" { found = 1; next }
found && /^## / { exit }
found && NF == 0 && !started { next }
found { started = 1; print }
