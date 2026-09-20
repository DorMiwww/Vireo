#!/usr/bin/env bash
set -e

# Vireo CLI Installer (macOS and Linux)
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/DorMiwww/Vireo/main/install.sh | bash

REPO="DorMiwww/Vireo"
INSTALL_DIR="${VIREO_INSTALL_DIR:-$HOME/.local/bin}"
SHARE_DIR="${VIREO_DATA_DIR:-$HOME/.local/share/vireo}"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m' # No Color

info() {
    printf "${BLUE}==>${NC} ${BOLD}%s${NC}\n" "$1"
}

success() {
    printf "${GREEN}==>${NC} ${BOLD}%s${NC}\n" "$1"
}

warn() {
    printf "${YELLOW}Warning:${NC} %s\n" "$1"
}

error() {
    printf "${RED}Error:${NC} %s\n" "$1" >&2
    exit 1
}

# 1. OS check
OS="$(uname -s)"
case "$OS" in
    Darwin)
        PLATFORM="macos"
        ;;
    Linux)
        PLATFORM="linux"
        ;;
    *)
        error "Operating system '$OS' is not supported by this installer. Vireo currently supports macOS and Linux."
        ;;
esac

info "Installing Vireo DaaC CLI for $PLATFORM..."

# 2. Check Java prerequisite
check_java() {
    if command -v java >/dev/null 2>&1; then
        JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{if ($1 == "1") print $2; else print $1}')
        if [ -n "$JAVA_VER" ] && [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
            warn "Detected Java version $JAVA_VER. Vireo requires Java 17 or newer."
        fi
    else
        warn "Java 17+ was not found in PATH. Vireo runs on the JVM."
        if [ "$PLATFORM" = "macos" ]; then
            warn "You can install Java via Homebrew: brew install openjdk@21"
        else
            warn "You can install Java via your package manager: e.g. sudo apt install openjdk-21-jre"
        fi
    fi
}
check_java

# 3. Create target directories
mkdir -p "$INSTALL_DIR"
mkdir -p "$SHARE_DIR"

# 4. Attempt to download pre-built release from GitHub
INSTALLED_FROM_RELEASE=false

info "Checking for latest release from GitHub..."
RELEASE_JSON=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" 2>/dev/null || true)

TAR_URL=$(echo "$RELEASE_JSON" | grep -o 'https://[^"]*vireo-[^"]*\.tar\.gz' | head -n 1 || true)
if [ -z "$TAR_URL" ]; then
    TAR_URL=$(echo "$RELEASE_JSON" | grep -o 'https://[^"]*vireo-[^"]*\.zip' | head -n 1 || true)
fi

if [ -n "$TAR_URL" ]; then
    info "Downloading pre-built release from $TAR_URL..."
    TMP_DL=$(mktemp -d)
    ARCHIVE_PATH="$TMP_DL/vireo_dist"
    if curl -fsSL "$TAR_URL" -o "$ARCHIVE_PATH"; then
        info "Extracting release to $SHARE_DIR..."
        rm -rf "$SHARE_DIR"/*
        if [[ "$TAR_URL" == *.zip ]]; then
            unzip -q "$ARCHIVE_PATH" -d "$TMP_DL/extracted"
        else
            mkdir -p "$TMP_DL/extracted"
            tar -xzf "$ARCHIVE_PATH" -C "$TMP_DL/extracted"
        fi
        
        EXTRACTED_SUBDIR=$(find "$TMP_DL/extracted" -maxdepth 1 -mindepth 1 -type d | head -n 1)
        if [ -n "$EXTRACTED_SUBDIR" ] && [ -d "$EXTRACTED_SUBDIR/bin" ]; then
            cp -R "$EXTRACTED_SUBDIR"/* "$SHARE_DIR/"
        else
            cp -R "$TMP_DL/extracted"/* "$SHARE_DIR/"
        fi
        rm -rf "$TMP_DL"
        INSTALLED_FROM_RELEASE=true
    else
        warn "Failed to download release asset. Falling back to source build."
        rm -rf "$TMP_DL"
    fi
fi

# 5. Fallback: Build from source if no release exists yet
if [ "$INSTALLED_FROM_RELEASE" = false ]; then
    info "No pre-built GitHub release found yet. Building from source via Gradle..."
    
    # If the installer is run from within the Vireo git repository, use current repo directly
    if [ -f "./gradlew" ] && [ -d "./vireo-cli" ]; then
        info "Using local repository to build..."
        ./gradlew :vireo-cli:installDist --no-daemon -q
        rm -rf "$SHARE_DIR"/*
        cp -R ./vireo-cli/build/install/vireo/* "$SHARE_DIR/"
    else
        if ! command -v git >/dev/null 2>&1; then
            error "git is required to build from source. Please install git."
        fi

        TMP_BUILD=$(mktemp -d)
        info "Cloning repository $REPO into temporary directory..."
        git clone --depth 1 "https://github.com/$REPO.git" "$TMP_BUILD/vireo" >/dev/null 2>&1

        info "Building Vireo CLI distribution..."
        (
            cd "$TMP_BUILD/vireo"
            ./gradlew :vireo-cli:installDist --no-daemon -q
        )

        rm -rf "$SHARE_DIR"/*
        cp -R "$TMP_BUILD/vireo/vireo-cli/build/install/vireo"/* "$SHARE_DIR/"
        rm -rf "$TMP_BUILD"
    fi
fi

# 6. Set executable permissions and create symlink
chmod +x "$SHARE_DIR/bin/vireo"
ln -sf "$SHARE_DIR/bin/vireo" "$INSTALL_DIR/vireo"

# 7. Verification and PATH check
success "Vireo has been successfully installed to $INSTALL_DIR/vireo!"

if [ -x "$INSTALL_DIR/vireo" ]; then
    VERSION_OUTPUT=$("$INSTALL_DIR/vireo" --version 2>/dev/null || true)
    if [ -n "$VERSION_OUTPUT" ]; then
        info "$VERSION_OUTPUT"
    fi
fi

# Check if INSTALL_DIR is in PATH
case ":$PATH:" in
    *":$INSTALL_DIR:"*) ;;
    *)
        echo ""
        warn "$INSTALL_DIR is not currently in your PATH."
        echo "To run 'vireo' from any directory, add it to your shell configuration:"
        echo ""
        CURRENT_SHELL=$(basename "$SHELL")
        if [ "$CURRENT_SHELL" = "zsh" ]; then
            echo "  echo 'export PATH=\"$INSTALL_DIR:\$PATH\"' >> ~/.zshrc"
            echo "  source ~/.zshrc"
        elif [ "$CURRENT_SHELL" = "bash" ]; then
            echo "  echo 'export PATH=\"$INSTALL_DIR:\$PATH\"' >> ~/.bashrc"
            echo "  source ~/.bashrc"
        else
            echo "  export PATH=\"$INSTALL_DIR:\$PATH\""
        fi
        echo ""
        ;;
esac

echo ""
info "Quick start:"
echo "  vireo init my-design-system"
echo "  cd my-design-system"
echo "  vireo render designs/card.dac --html --out card.html"
echo ""
