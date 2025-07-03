# ~/.bashrc - Bash Configuration with Git Enhancements

# Color Variables
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
NC='\033[0m' # No Color

# Git Branch in Prompt
parse_git_branch() {
    git branch 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/(\1)/'
}

# Enhanced PS1 with Git Branch
export PS1="\[\033[32m\]\u@\h\[\033[00m\]:\[\033[34m\]\w\[\033[31m\]\$(parse_git_branch)\[\033[00m\]\$ "

# Git Aliases
alias g='git'
alias gs='git status'
alias ga='git add'
alias gaa='git add .'
alias gap='git add -p'
alias gc='git commit'
alias gcm='git commit -m'
alias gca='git commit --amend'
alias gcan='git commit --amend --no-edit'

# Branch Management
alias gb='git branch'
alias gba='git branch -a'
alias gbd='git branch -d'
alias gbD='git branch -D'
alias gco='git checkout'
alias gcb='git checkout -b'
alias gsw='git switch'
alias gswc='git switch -c'

# Remote Operations
alias gf='git fetch'
alias gfo='git fetch origin'
alias gp='git push'
alias gpu='git push -u origin $(git branch --show-current)'
alias gpl='git pull'
alias gplr='git pull --rebase'

# Stash Operations
alias gst='git stash'
alias gstp='git stash pop'
alias gsta='git stash apply'
alias gstl='git stash list'
alias gsts='git stash show -p'
alias gstd='git stash drop'

# Log and History
alias gl='git log --oneline --graph --decorate'
alias gla='git log --oneline --graph --decorate --all'
alias glp='git log -p'
alias gll='git log --pretty=format:"%C(yellow)%h%Cred%d %Creset%s%Cblue [%cn]" --decorate --numstat'

# Diff Operations
alias gd='git diff'
alias gds='git diff --staged'
alias gdh='git diff HEAD'

# Reset and Clean
alias grh='git reset HEAD'
alias grhh='git reset --hard HEAD'
alias gclean='git clean -fd'
alias gpristine='git reset --hard && git clean -fd'

# Cherry Pick
alias gcp='git cherry-pick'
alias gcpa='git cherry-pick --abort'
alias gcpc='git cherry-pick --continue'

# Rebase
alias grb='git rebase'
alias grbi='git rebase -i'
alias grbc='git rebase --continue'
alias grba='git rebase --abort'

# Merge
alias gm='git merge'
alias gma='git merge --abort'
alias gmff='git merge --ff-only'
alias gmnff='git merge --no-ff'

# Functions
# Interactive branch deletion
gbd-interactive() {
    git branch | grep -v "main\|master\|develop" | xargs -n 1 -p git branch -d
}

# Show commits between branches
gcompare() {
    git log --oneline --graph --decorate --left-right "$1"..."$2"
}

# Quick commit with generated message
gquick() {
    git add .
    git commit -m "WIP: $(date +%Y-%m-%d\ %H:%M:%S)"
}

# Git flow shortcuts
gfeature() {
    git checkout -b "feature/$1"
}

gbugfix() {
    git checkout -b "bugfix/$1"
}

ghotfix() {
    git checkout -b "hotfix/$1"
}

# Show file history
gfile() {
    git log --follow -p -- "$1"
}

# Undo last commit but keep changes
gundo() {
    git reset --soft HEAD~1
}

# Show authors
gauthors() {
    git shortlog -sn
}

# Find string in git history
gfind() {
    git grep "$1" $(git rev-list --all)
}

# Oh My Zsh Configuration (.zshrc)
# Install Oh My Zsh first:
# sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

# Path to oh-my-zsh installation
export ZSH="$HOME/.oh-my-zsh"

# Theme (recommended: powerlevel10k)
ZSH_THEME="powerlevel10k/powerlevel10k"

# Plugins
plugins=(
    git
    git-flow
    docker
    kubectl
    npm
    yarn
    android
    gradle
    zsh-autosuggestions
    zsh-syntax-highlighting
    z
    colored-man-pages
    command-not-found
    extract
)

source $ZSH/oh-my-zsh.sh

# Additional Git aliases for Zsh
alias gcl='git clone --recurse-submodules'
alias gwip='git add -A; git rm $(git ls-files --deleted) 2> /dev/null; git commit --no-verify -m "🚧 WIP"'
alias gunwip='git log -n 1 | grep -q -c "🚧 WIP" && git reset HEAD~1'

# Pretty git log
alias glg='git log --graph --pretty=format:"%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset" --abbrev-commit'

# Git configuration (~/.gitconfig)
cat > ~/.gitconfig << 'EOF'
[user]
    name = Your Name
    email = your.email@example.com

[core]
    editor = vim
    whitespace = fix,-indent-with-non-tab,trailing-space,cr-at-eol
    pager = less -FRSX

[color]
    ui = auto
    diff = auto
    status = auto
    branch = auto
    interactive = auto

[alias]
    # Status and info
    s = status -s
    st = status
    
    # Adding
    a = add
    aa = add .
    ap = add -p
    
    # Committing
    c = commit
    cm = commit -m
    ca = commit --amend
    can = commit --amend --no-edit
    
    # Branching
    b = branch
    ba = branch -a
    bd = branch -d
    bD = branch -D
    co = checkout
    cob = checkout -b
    
    # Remotes
    f = fetch
    p = push
    pl = pull
    pr = pull --rebase
    
    # Logging
    l = log --oneline --graph
    la = log --oneline --graph --all
    lg = log --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset'
    
    # Diffs
    d = diff
    ds = diff --staged
    dc = diff --cached
    
    # Stashing
    ss = stash save
    sp = stash pop
    sl = stash list
    
    # Reset
    unstage = reset HEAD --
    undo = reset --soft HEAD~1
    
    # Cherry pick
    cp = cherry-pick
    
    # Rebase
    rb = rebase
    rbi = rebase -i
    rbc = rebase --continue
    rba = rebase --abort
    
    # Useful shortcuts
    last = log -1 HEAD
    visual = !gitk
    tags = tag -l
    branches = branch -a
    remotes = remote -v
    
    # Show contributors
    contributors = shortlog -sn
    
    # Find commits by message
    find = log --pretty=\"format:%Cgreen%H%Creset %s\" --grep
    
    # Aliases for typos
    statis = status
    stats = status
    statsu = status
    stauts = status

[push]
    default = current
    followTags = true

[pull]
    rebase = true

[merge]
    tool = vimdiff

[diff]
    tool = vimdiff

[init]
    defaultBranch = main

[fetch]
    prune = true

[rebase]
    autoStash = true
EOF

# Install required Zsh plugins
echo "To complete setup, install these plugins:"
echo "git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions"
echo "git clone https://github.com/zsh-users/zsh-syntax-highlighting.git ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting"
echo "git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ${ZSH_CUSTOM:-$HOME/.oh-my-zsh/custom}/themes/powerlevel10k"