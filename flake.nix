# This flake's package is tied to SpMp release binaries. It does not build from source.
{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    android-nixpkgs.url = "github:HPRIOR/android-nixpkgs/d144e1aff31d45e92ee981e04d871b000fd791f9";
  };

  outputs = { self, nixpkgs, android-nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
      };

      runtime_jdk = pkgs.jdk22;

      runtime_packages = with pkgs; [
        runtime_jdk
        libglvnd
        xorg.libX11
        fontconfig
        mpv
        vulkan-loader
        xorg.libXtst
        apksigcopier

        # Webview
        at-spi2-atk
        cups.lib
        mesa
        pango
      ];

      android-sdk = (android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
        cmdline-tools-latest
        platform-tools
        build-tools-35-0-0
        platforms-android-35
        build-tools-34-0-0
        platforms-android-34
      ]));

      spmp_package =
        let
          pname = "spmp";
          version = "0.4.1";
        in
        pkgs.stdenv.mkDerivation {
          inherit pname version;

          src = pkgs.fetchurl {
            url = "https://github.com/toasterofbread/spmp/releases/download/v${version}/spmp-v${version}-linux-x86_64.tar.gz";
            hash = "sha256-Du66p0Zo4OQH7dAD2Dz3KyGa4js4DNCG91rYim52G5Q=";
          };

          nativeBuildInputs = with pkgs; [
            autoPatchelfHook
            gnutar
          ];

          buildInputs = with pkgs; [
            gnome2.gnome_vfs
            gnome2.GConf
          ] ++ runtime_packages;

          installPhase = ''
            tar -xzf $src

            mkdir -p $out/dist
            mv ./bin $out/dist/bin
            mv ./lib $out/dist/lib

            lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
            lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")

            mkdir -p $out/bin
            echo "#!/bin/sh" >> $out/bin/spmp
            echo "LD_LIBRARY_PATH=\"$lib_paths_str:\$LD_LIBRARY_PATH\" $out/dist/bin/spmp \"\$@\"" >> $out/bin/spmp
            chmod +x $out/bin/spmp
          '';
        };
    in
    {
      packages."${system}".default = spmp_package;

      devShells."${system}".default =
        pkgs.mkShell {
          packages = with pkgs; [
            jdk21
            jdk22
            android-sdk
            appimagekit
            appstream
            zsync

            # For testing new releases
            # spmp_package
          ] ++ runtime_packages;

          JAVA_21_HOME = "${pkgs.jdk21}/lib/openjdk";
          JAVA_22_HOME = "${pkgs.jdk22}/lib/openjdk";
          JAVA_HOME = "${pkgs.jdk22}/lib/openjdk";

          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${android-sdk}/share/android-sdk/build-tools/34.0.0/aapt2";

          shellHook = ''
            # Add NIX_LDFLAGS to LD_LIBRARY_PATH
            lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
            lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")
            export LD_LIBRARY_PATH="$lib_paths_str:$LD_LIBRARY_PATH"
          '';
        };
    };
}
