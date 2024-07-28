{
  description = "SpMp development environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    android-nixpkgs.url = "github:HPRIOR/android-nixpkgs/516bd59caa6883d1a5dad0538af03a1f521e7764";
  };

  outputs = { self, nixpkgs, android-nixpkgs, ... }:
    let
      system = "x86_64-linux";
    in
    {
      devShells."${system}".default =
        let
          pkgs = import nixpkgs {
            inherit system;
          };

          android-sdk = (android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
            cmdline-tools-latest
            build-tools-34-0-0
            platform-tools
            platforms-android-34
          ]));
        in
        pkgs.mkShell {
          packages = with pkgs; [
            jdk21
            jdk22
            android-sdk
            appimagekit
            appstream
            zsync

            # Runtime
            libglvnd
            xorg.libX11
            fontconfig
            mpv
            vulkan-loader
          ];

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
