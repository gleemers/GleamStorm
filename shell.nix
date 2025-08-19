{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    kotlin
    gradle
    jdk21
    nodejs
    android-tools
    jetbrains.idea-ultimate
    xorg.libX11
    xorg.libXext
    xorg.libXtst
    xorg.libXi
    xorg.libXrandr
    xorg.libXrender
    xorg.libXfixes
    xorg.libXcursor
    gtk3
    gtk2
    glib
    cairo
    pango
    gdk-pixbuf
    atk
    libGL
    libGLU
    fontconfig
    freetype
    zlib
    libpng
    libjpeg
    expat
    libxkbcommon
    dbus
    at-spi2-atk
    at-spi2-core
  ];

  shellHook = ''
    export JAVA_HOME=${pkgs.jdk21}
    export ANDROID_HOME=$HOME/Android/Sdk
    export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH

    export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath [
      pkgs.xorg.libX11
      pkgs.xorg.libXext
      pkgs.xorg.libXtst
      pkgs.xorg.libXi
      pkgs.xorg.libXrandr
      pkgs.xorg.libXrender
      pkgs.xorg.libXfixes
      pkgs.xorg.libXcursor
      pkgs.gtk3
      pkgs.gtk2
      pkgs.glib
      pkgs.cairo
      pkgs.pango
      pkgs.gdk-pixbuf
      pkgs.atk
      pkgs.libGL
      pkgs.libGLU
      pkgs.fontconfig
      pkgs.freetype
      pkgs.zlib
      pkgs.libpng
      pkgs.libjpeg
      pkgs.expat
      pkgs.libxkbcommon
      pkgs.dbus
      pkgs.at-spi2-atk
      pkgs.at-spi2-core
    ]}:$LD_LIBRARY_PATH

    export GTK_PATH=${pkgs.gtk3}/lib/gtk-3.0:${pkgs.gtk2}/lib/gtk-2.0
    export GDK_PIXBUF_MODULE_FILE=${pkgs.gdk-pixbuf}/lib/gdk-pixbuf-2.0/2.10.0/loaders.cache
  '';
}