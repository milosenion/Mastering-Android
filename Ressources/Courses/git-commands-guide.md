# Complete Git Commands Guide

## Table of Contents
1. [Git Areas and Workflow](#git-areas-and-workflow)
2. [Essential Commands](#essential-commands)
3. [Branching Strategies](#branching-strategies)
4. [Stashing](#stashing)
5. [Cherry Picking](#cherry-picking)
6. [Rebasing](#rebasing)
7. [Advanced Techniques](#advanced-techniques)
8. [Best Practices](#best-practices)

## Git Areas and Workflow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Working        │     │    Staging      │     │   Local         │     │    Remote       │
│  Directory      │     │    Area         │     │   Repository    │     │   Repository    │
│                 │ add │   (Index)       │commit│   (.git)        │ push│   (GitHub)      │
│  Your files     ├────►│  Ready to       ├─────►│  Committed      ├────►│   Shared with   │
│  as you edit    │     │  commit         │     │  history        │     │   team          │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Essential Commands

### Basic Operations
```bash
# Initialize repository
git init

# Clone repository
git clone <url>

# Check status
git status

# View differences
git diff                    # Working directory vs staging
git diff --staged          # Staging vs last commit
git diff HEAD~1 HEAD       # Compare commits

# Add files to staging
git add <file>             # Add specific file
git add .                  # Add all files
git add -p                 # Add interactively
git add *.kt              # Add by pattern

# Commit changes
git commit -m "message"
git commit -am "message"   # Add and commit (tracked files only)
git commit --amend        # Modify last commit
```

### Branching
```bash
# Create and switch branches
git branch feature/login          # Create branch
git checkout feature/login        # Switch to branch
git checkout -b feature/login     # Create and switch
git switch -c feature/login       # Modern syntax

# List branches
git branch                        # Local branches
git branch -r                     # Remote branches
git branch -a                     # All branches

# Delete branches
git branch -d feature/done        # Delete merged branch
git branch -D feature/force       # Force delete
git push origin --delete feature  # Delete remote branch

# Merge branches
git checkout main
git merge feature/login           # Merge feature into main
git merge --no-ff feature        # Force merge commit
```

## Branching Strategies

### Feature Branch Workflow
```
main     ──●──●──●──────────●── (merge)
            \              /
feature      ●──●──●──●──●

Commands:
git checkout -b feature/star-wars-ui
# Make commits
git push -u origin feature/star-wars-ui
# Create PR, review, merge
```

### Git Flow
```
main     ──●────────────────●── (release)
            \              /
develop  ────●──●──●──●──●──●──
              \  /    \  /
feature        ●●      ●●
```

## Stashing

### Stash Workflow
```
Working Directory (changes)
         │
         │ git stash
         ▼
    Stash Stack
    ┌─────────┐
    │stash@{0}│ ← Latest
    │stash@{1}│
    │stash@{2}│
    └─────────┘
```

### Stash Commands
```bash
# Save changes
git stash                         # Quick stash
git stash save "WIP: login form" # With message
git stash -u                      # Include untracked files
git stash -k                      # Keep staged files

# Retrieve changes
git stash pop                     # Apply and remove latest
git stash apply                   # Apply but keep in stash
git stash apply stash@{2}        # Apply specific stash

# Manage stashes
git stash list                    # Show all stashes
git stash show -p stash@{0}      # Show stash contents
git stash drop stash@{1}         # Delete specific stash
git stash clear                   # Delete all stashes

# Create branch from stash
git stash branch feature/saved stash@{0}
```

## Cherry Picking

### Cherry Pick Scenarios

#### Single Commit
```
main:     A──B──C──D──E
               ↓ (cherry-pick C)
feature:  X──Y──C'

git checkout feature
git cherry-pick C
```

#### Multiple Commits
```
main:     A──B──C──D──E──F
               ↓    ↓ (cherry-pick C and E)
feature:  X──Y──C'──E'

git cherry-pick C E
```

#### Squashing Multiple Commits
```bash
# Cherry-pick without committing
git cherry-pick -n commit1
git cherry-pick -n commit2
git cherry-pick -n commit3

# Commit all as one
git commit -m "Combined features from multiple commits"
```

#### Complex Example: Mixing Old and New
```bash
# Scenario: Want commits A (old), skip B and C, then D and E (new)
# Current branch: feature
# Source branch: main

# Method 1: Individual picks
git cherry-pick A
git cherry-pick D E

# Method 2: Range with exclusion
git cherry-pick A
git cherry-pick C..E  # Picks D and E (excludes C)

# Method 3: Interactive rebase alternative
git rebase -i main
# Mark commits to pick/skip
```

## Rebasing

### Interactive Rebase
```bash
git rebase -i HEAD~3

# Opens editor with:
pick abc123 Add login
pick def456 Fix bug
pick ghi789 Update styles

# Change to:
pick abc123 Add login
squash def456 Fix bug
reword ghi789 Update UI styles

# Commands:
# p, pick = use commit
# r, reword = change message
# e, edit = stop to amend
# s, squash = meld into previous
# f, fixup = like squash but discard message
# d, drop = remove commit
```

### Rebase vs Merge
```
# Merge (preserves history)
      A──B──C topic
     /        \
D──E──F──G──H──M main

# Rebase (linear history)
              A'─B'─C' topic
             /
D──E──F──G──H main
```

## Advanced Techniques

### Reflog - Recovery Tool
```bash
git reflog                    # Show all actions
git checkout HEAD@{2}         # Go to previous state
git reset --hard HEAD@{1}     # Restore lost commits
```

### Bisect - Find Bad Commits
```bash
git bisect start
git bisect bad                # Current commit is bad
git bisect good v1.0          # v1.0 was good
# Git checks out middle commit
git bisect good/bad           # Mark as good or bad
# Repeat until bad commit found
git bisect reset              # End bisect
```

### Worktrees - Multiple Working Directories
```bash
git worktree add ../hotfix main
git worktree add ../feature feature/login
git worktree list
git worktree remove ../hotfix
```

## Best Practices

### Commit Messages
```
feat: Add Star Wars character filter
^──^  ^────────────────────────────^
│     │
│     └─⫸ Summary in present tense
│
└─⫸ Type: feat|fix|docs|style|refactor|test|chore

Body: Detailed explanation of what and why
- Bullet points for multiple changes
- Reference issues: Fixes #123
```

### Branch Naming
```
feature/add-login-screen
bugfix/fix-navigation-crash
hotfix/security-patch
release/v2.0.0
```

### Commit Best Practices
1. Commit early and often
2. One logical change per commit
3. Write meaningful messages
4. Test before committing
5. Never commit sensitive data

### Undo Operations
```bash
# Undo last commit, keep changes
git reset --soft HEAD~1

# Undo last commit, discard changes
git reset --hard HEAD~1

# Revert commit (safe for pushed commits)
git revert abc123

# Clean untracked files
git clean -fd
```