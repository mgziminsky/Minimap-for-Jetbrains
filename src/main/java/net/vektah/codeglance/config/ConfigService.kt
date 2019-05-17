package net.vektah.codeglance.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MiniMap",
    storages = [Storage("CodeGlance.xml")]
)
class ConfigService : PersistentStateComponent<Config> {
    private val observers : MutableSet<() -> Unit> = hashSetOf()
    private val config = Config()

    override fun getState(): Config? = config
    public fun addOnChange(f :() -> Unit) = observers.add(f)
    public fun removeOnChange(f :() -> Unit) = observers.remove(f)

    public fun notifyChange() {
        observers.forEach {
            it()
        }
    }

    override fun loadState(config: Config) {
        XmlSerializerUtil.copyBean(config, this.config)
    }
}
