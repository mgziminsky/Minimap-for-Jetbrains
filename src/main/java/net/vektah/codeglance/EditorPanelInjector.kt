package net.vektah.codeglance

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import net.vektah.codeglance.config.Config
import net.vektah.codeglance.config.ConfigService
import javax.swing.*
import java.awt.*

/**
 * Injects a panel into any newly created editors.
 */
class EditorPanelInjector(private val project: Project) : FileEditorManagerAdapter() {
    private val logger = Logger.getInstance(javaClass)
    private var config: Config = ServiceManager.getService(ConfigService::class.java).state!!

    override fun fileOpened(fem: FileEditorManager, virtualFile: VirtualFile) {
        // Seems there is a case where multiple split panes can have the same file open and getSelectedEditor, and even
        // getEditors(virtualFile) return only one of them... So shotgun approach here.
        val editors = fem.allEditors
        for (editor in editors) {
            inject(editor)
        }
    }

    /**
     * Here be dragons. No Seriously. Run!
     *
     * We are digging way down into the editor layout. This lets the codeglance panel be right next to the scroll bar.
     * In an ideal world it would be inside the scroll bar... maybe one day.
     *
     * vsch: added handling when the editor is even deeper, inside firstComponent of a JBSplitter, used by idea-multimarkdown
     * and Markdown Support to show split preview. Missed this plugin while editing markdown. These changes got it back.
     *
     * @param editor A text editor to inject into.
     */
    private fun getPanel(editor: FileEditor): JPanel? {
        if (editor !is TextEditor) {
            logger.debug("I01: Injection failed, only text editors are supported currently.")
            return null
        }

        try {
            val outerPanel = editor.component as JPanel
            val outerLayout = outerPanel.layout as BorderLayout
            var layoutComponent = outerLayout.getLayoutComponent(BorderLayout.CENTER)

            if (layoutComponent is JBSplitter) {
                // editor is inside firstComponent of a JBSplitter
                val editorComp = layoutComponent.firstComponent as JPanel
                layoutComponent = (editorComp.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER)
            }

            val pane = layoutComponent as JLayeredPane

            return when {
                pane.componentCount > 1 -> pane.getComponent(1)
                else -> pane.getComponent(0)
            } as JPanel
        } catch (e: ClassCastException) {
            logger.warn("Injection failed")
            e.printStackTrace()
            return null
        }
    }

    private fun inject(editor: FileEditor) {
        val panel = getPanel(editor) ?: return
        val innerLayout = panel.layout as BorderLayout

        val where = if (config.isRightAligned)
            BorderLayout.LINE_END
        else
            BorderLayout.LINE_START

        if (innerLayout.getLayoutComponent(where) == null) {
            val glancePanel = GlancePanel(project, editor)
            panel.add(glancePanel, where)
            // Is this really necessary???
            Disposer.register(editor, Disposable { panel.remove(glancePanel) })
        }
    }
}
