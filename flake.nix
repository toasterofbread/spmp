{
  description = "SpMp development environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
  };

  outputs = { self, nixpkgs, ... }:
    let
      system = "x86_64-linux";
    in
    {
      devShells."${system}".default =
        let
          pkgs = import nixpkgs {
            inherit system;
          };
        in
        pkgs.mkShell {
          packages = with pkgs; [
            jdk21
            jdk22

            # Runtime
            libglvnd
            xorg.libX11
            fontconfig
            mpv
          ];

          JAVA_21_HOME = "${pkgs.jdk21}/lib/openjdk";
          JAVA_22_HOME = "${pkgs.jdk22}/lib/openjdk";
          JAVA_HOME = "${pkgs.jdk22}/lib/openjdk";

          shellHook = ''
            # Add NIX_LDFLAGS to LD_LIBRARY_PATH
            lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
            lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")
            export LD_LIBRARY_PATH="$lib_paths_str:$LD_LIBRARY_PATH"

            export X11_DISPLAY=$DISPLAY
          '';
        };
    };
}
