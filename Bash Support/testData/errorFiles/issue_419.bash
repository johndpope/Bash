local -a issues=($(git hub issues --parent 2>/dev/null || git hub issues 2>/dev/null))