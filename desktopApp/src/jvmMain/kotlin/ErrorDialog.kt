import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class ExceptionDialog(owner: Frame, exception: Throwable): Dialog(owner, "Error", true) {
    init {
        layout = BorderLayout()

        val text_area: TextArea = TextArea()
        text_area.text = exception.toString()
        text_area.append("\n\n")
        text_area.append("Stack trace:\n")
        exception.stackTraceToString().lines().forEach { text_area.append("$it\n") }
        add(text_area, BorderLayout.CENTER)

        val close_button: Button = Button("Close")
        close_button.addActionListener { e: ActionEvent -> dispose() }

        val copy_button: Button = Button("Copy error")
        copy_button.addActionListener {
            val selection: StringSelection = StringSelection(text_area.text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        }

        val button_panel: Panel = Panel()
        button_panel.add(copy_button)
        button_panel.add(close_button)
        add(button_panel, BorderLayout.SOUTH)

        pack()
    }
}
