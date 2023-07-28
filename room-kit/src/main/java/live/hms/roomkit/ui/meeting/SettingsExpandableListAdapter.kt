package live.hms.roomkit.ui.meeting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import live.hms.roomkit.R


class SettingsExpandableListAdapter(var context: Context, prebuiltDebugMode: Boolean) :
    BaseExpandableListAdapter() {

    enum class MeetingLayout {
        ActiveSpeaker, HeroMode, GridView
    }

    val list = arrayListOf(
        MeetingLayout.ActiveSpeaker,
        if (prebuiltDebugMode) MeetingLayout.HeroMode else null,
        MeetingLayout.GridView
    ).filterNotNull()

    override fun getGroupCount(): Int = 1

    override fun getChildrenCount(groupPosition: Int): Int = list.size

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroup(groupPosition: Int): String {
        return "Layout"
    }

    override fun getChild(groupPosition: Int, childPosition: Int): String {
        return list[childPosition].toString()
    }

    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertViewParent: View?, parent: ViewGroup?
    ): View {
        val title: String = getGroup(groupPosition)
        var convertView = convertViewParent
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.expanded_list_parent_view, null, false)
        }
        val expandedListTextView = convertView?.findViewById<TextView>(R.id.tv_title)
        expandedListTextView?.text = title
        return convertView!!
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertViewParent: View?,
        parent: ViewGroup?
    ): View {
        val title: String = getChild(groupPosition, childPosition)
        var convertView = convertViewParent
        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.expanded_list_child_view, null, false)
        }
        val expandedListTextView = convertView?.findViewById<TextView>(R.id.expandedListItem)
        expandedListTextView?.text = title
        return convertView!!
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}