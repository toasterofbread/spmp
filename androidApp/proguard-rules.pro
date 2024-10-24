-dontobfuscate
-keep class com.toasterofbread.spmp.** { *; }

# Klaxon
-keep class kotlin.reflect.jvm.internal.**
-keep class com.beust.klaxon.** { *; }
-keep interface com.beust.klaxon.** { *; }
-keep class kotlin.Metadata { *; }

# KizzyRPC
# See https://github.com/google/gson/blob/6d9c3566b71900c54644a9f71ce028696ee5d4bd/examples/android-proguard-example/proguard.cfg
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# F-Droid
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Kuromoji
-keep class com.atilika.kuromoji.** { *; }

# JAudioTagger
-dontwarn org.jaudiotagger.**
-keep class org.jaudiotagger.** { *; }

# Ktor
-dontwarn io.ktor.**

-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# From proguard-android-optimize.txt

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ???
-dontwarn java.awt.AWTEvent
-dontwarn java.awt.Component
-dontwarn java.awt.Container
-dontwarn java.awt.EventQueue
-dontwarn java.awt.MenuComponent
-dontwarn java.awt.event.ActionListener
-dontwarn java.awt.event.ComponentListener
-dontwarn java.awt.event.ContainerListener
-dontwarn java.awt.event.KeyAdapter
-dontwarn java.awt.event.KeyListener
-dontwarn java.awt.event.MouseAdapter
-dontwarn java.awt.event.MouseListener
-dontwarn java.awt.event.WindowAdapter
-dontwarn javax.swing.CellEditor
-dontwarn javax.swing.DesktopManager
-dontwarn javax.swing.JComboBox
-dontwarn javax.swing.JComponent
-dontwarn javax.swing.JDesktopPane
-dontwarn javax.swing.JDialog
-dontwarn javax.swing.JFrame
-dontwarn javax.swing.JInternalFrame
-dontwarn javax.swing.JLabel
-dontwarn javax.swing.JMenu
-dontwarn javax.swing.JMenuBar
-dontwarn javax.swing.JMenuItem
-dontwarn javax.swing.JOptionPane
-dontwarn javax.swing.JPanel
-dontwarn javax.swing.JPopupMenu
-dontwarn javax.swing.JScrollPane
-dontwarn javax.swing.JTable
-dontwarn javax.swing.JTextArea
-dontwarn javax.swing.JToolBar
-dontwarn javax.swing.JTree
-dontwarn javax.swing.JViewport
-dontwarn javax.swing.SwingUtilities
-dontwarn javax.swing.UIManager
-dontwarn javax.swing.event.DocumentListener
-dontwarn javax.swing.event.EventListenerList
-dontwarn javax.swing.event.InternalFrameAdapter
-dontwarn javax.swing.event.ListSelectionListener
-dontwarn javax.swing.event.PopupMenuListener
-dontwarn javax.swing.event.TreeExpansionListener
-dontwarn javax.swing.event.TreeModelListener
-dontwarn javax.swing.filechooser.FileFilter
-dontwarn javax.swing.table.AbstractTableModel
-dontwarn javax.swing.table.TableCellEditor
-dontwarn javax.swing.table.TableCellRenderer
-dontwarn javax.swing.text.BadLocationException
-dontwarn javax.swing.tree.DefaultTreeSelectionModel
-dontwarn javax.swing.tree.TreeModel
