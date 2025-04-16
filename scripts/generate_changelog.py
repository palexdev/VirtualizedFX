from collections import defaultdict, OrderedDict
import subprocess
import sys

# Helper function to run shell commands
def run_command(command):
    try:
        result = subprocess.run(command, check=True, capture_output=True, text=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {command}\n{e.stderr}", file=sys.stderr)
        return None

# Fetch commits from the latest two tags
def get_commits_between_tags():
    try:
        last_tag = run_command(["git", "describe", "--tags", "HEAD^", "--abbrev=0"])
        if not last_tag:
            return []

        log_output = run_command(["git", "log", f"{last_tag}..HEAD", "--oneline"])
        if not log_output:
            return []

        return log_output.splitlines()
    except Exception as e:
        print(f"Error fetching commits: {e}", file=sys.stderr)
        return []

# Parse commits into a structured changelog
def parse_commits(commits):
    changelog = defaultdict(lambda: {
        ":sparkles: Added": [],
        ":recycle: Changed": [],
        ":bug: Fixed": [],
        ":wrench: Misc": []
    })

    gitmoji_categories = {
        ":sparkles:": ":sparkles: Added",
        ":boom:": ":sparkles: Added",
        ":recycle:": ":recycle: Changed",
        ":bug:": ":bug: Fixed",
    }

    for commit in commits:
        parts = commit.split(" ")

        commit_hash = parts[0]
        module_name = "project"
        for part in parts:
            if part.startswith("[") and part.endswith("]"):
                module_name = part[1:-1]
                break

        gitmoji = None
        for part in parts:
            if part.startswith(":") and part.endswith(":"):
                gitmoji = part
                break

        gitmoji_index = parts.index(gitmoji) if gitmoji else 1
        commit_message = " ".join(parts[gitmoji_index + 1:]) if gitmoji_index else ""

        if gitmoji == ":bookmark:":
            continue

        category = gitmoji_categories.get(gitmoji, ":wrench: Misc")
        changelog[module_name][category].append(f"{commit_hash}: {commit_message}")

    return changelog

# Generate markdown from changelog data
def generate_markdown(changelog):
    sorted_changelog = OrderedDict()
    sorted_changelog["project"] = changelog.pop("project", {})
    for module in sorted(changelog.keys()):
        sorted_changelog[module] = changelog[module]

    markdown = ""

    for module, categories in sorted_changelog.items():
        if module == "project":
            markdown += "# Project\n\n"
        else:
            markdown += f"# Module: {module.capitalize()}\n\n"

        for category, messages in categories.items():
            if messages:
                markdown += f"## {category}\n"
                for message in messages:
                    markdown += f"- {message}\n"
        markdown += "\n"

    return markdown

# Main function
def main():
    try:
        commits = get_commits_between_tags()

        if not commits:
            print("No commits found or error fetching commits.")
            with open("Changelog.md", "w") as file:
                file.write("")
            return

        changelog = parse_commits(commits)
        markdown = generate_markdown(changelog)

        with open("Changelog.md", "w") as file:
            file.write(markdown)

        print("Changelog written to Changelog.md")
    except Exception as e:
        print(f"Unexpected error: {e}", file=sys.stderr)
        with open("Changelog.md", "w") as file:
            file.write("")

if __name__ == "__main__":
    main()
