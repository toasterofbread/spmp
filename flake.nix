# This flake's package is tied to SpMp release binaries. It does not build from source.
{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    android-nixpkgs.url = "github:HPRIOR/android-nixpkgs/516bd59caa6883d1a5dad0538af03a1f521e7764";
  };

  outputs = { self, nixpkgs, android-nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
      };

      runtime_jdk = pkgs.jdk22;

      android-sdk = (android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
        cmdline-tools-latest
        build-tools-34-0-0
        platform-tools
        platforms-android-34
      ]));

      runtime_packages = with pkgs; [
        runtime_jdk
        libglvnd
        xorg.libX11
        fontconfig
        mpv
        vulkan-loader
      ];
    in
    {
      packages."${system}".default =
        let
          src = pkgs.fetchurl {
            url = "https://github.com/toasterofbread/spmp/releases/download/v0.4.0-RC1/spmp-v0.4.0-RC1-linux-x86_64.appimage";
            hash = "sha256-Cs/oln8mW8TSzeBCoqKFDz0NYGH5aJ66DXArjA36SWI=";
          };

          pname = "spmp";
          version = "0.4.0-RC1";
        in
        pkgs.stdenv.mkDerivation {
          inherit pname version;

          src = pkgs.appimageTools.extract {
            inherit src pname version;
          };

          nativeBuildInputs = with pkgs; [
            autoPatchelfHook
          ];

          buildInputs = with pkgs; [
            gnome2.gnome_vfs
            gnome2.GConf
          ] ++ runtime_packages;

          installPhase = ''
            mkdir -p $out/appimage
            cp -as $src/bin $out/appimage/bin
            cp -as $src/lib $out/appimage/lib

            lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
            lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")

            mkdir -p $out/bin
            echo "#!/bin/sh" >> $out/bin/spmp
            echo "LD_LIBRARY_PATH=\"$lib_paths_str:\$LD_LIBRARY_PATH\" $out/appimage/bin/spmp" >> $out/bin/spmp
            chmod +x $out/bin/spmp
          '';
        };

      devShells."${system}".default =
        pkgs.mkShell {
          packages = with pkgs; [
            jdk21
            jdk22
            android-sdk
            appimagekit
            appstream
            zsync
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
