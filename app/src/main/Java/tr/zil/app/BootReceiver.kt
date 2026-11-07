package tr.zil.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Store.enabled(context) && Store.bells(context).isNotEmpty()) {
            ServiceCtl.start(context)
        }
    }
}
