{
  description = "SpMp development environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    android-nixpkgs = {
      url = "github:HPRIOR/android-nixpkgs/516bd59caa6883d1a5dad0538af03a1f521e7764";
      #follows = "nixpkgs";
    };
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
            jdk17
            android-sdk

            # Runtime
            libglvnd
            xorg.libX11
            fontconfig
          ];

          JAVA_HOME = "${pkgs.jdk17}/lib/openjdk";

          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${android-sdk}/share/android-sdk/build-tools/34.0.0/aapt2";
        };
    };
}
