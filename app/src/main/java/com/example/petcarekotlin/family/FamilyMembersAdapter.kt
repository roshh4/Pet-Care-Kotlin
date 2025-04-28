package com.example.petcarekotlin.family

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R

/**
 * Data class for member information to display in the list
 */
data class MemberInfo(
    val userId: String,
    val username: String,
    val email: String = "",
    val isAdmin: Boolean = false,
    val isOwner: Boolean = false,
    val isCurrentUser: Boolean = false
)

/**
 * Adapter for displaying family members in a RecyclerView
 */
class FamilyMembersAdapter(
    private val onMakeAdminClicked: (String) -> Unit,
    private val onRemoveMemberClicked: (String) -> Unit,
    private val onMakeOwnerClicked: (String) -> Unit
) : RecyclerView.Adapter<FamilyMembersAdapter.MemberViewHolder>() {

    private var members: List<MemberInfo> = emptyList()
    private var currentUserId: String = ""
    private var isCurrentUserAdmin: Boolean = false
    private var isCurrentUserOwner: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_family_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        
        // Set member name
        holder.memberName.text = member.username
        
        // Set role badge
        when {
            member.isOwner -> {
                holder.roleBadge.visibility = View.VISIBLE
                holder.roleBadge.text = "Owner"
                holder.roleBadge.setBackgroundResource(R.drawable.badge_owner)
            }
            member.isAdmin -> {
                holder.roleBadge.visibility = View.VISIBLE
                holder.roleBadge.text = "Admin"
                holder.roleBadge.setBackgroundResource(R.drawable.badge_admin)
            }
            else -> holder.roleBadge.visibility = View.GONE
        }
        
        // Set email or "You" for current user
        holder.memberEmail.text = if (member.isCurrentUser) "You" else member.email
        
        // Handle options menu for other members (not self)
        if (member.isCurrentUser) {
            holder.optionsButton.visibility = View.GONE
        } else {
            holder.optionsButton.visibility = View.VISIBLE
            holder.optionsButton.setOnClickListener { view ->
                showMemberOptions(view, member)
            }
        }
    }

    private fun showMemberOptions(view: View, member: MemberInfo) {
        val context = view.context
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.member_options_menu, popup.menu)
        
        // Configure menu items based on permissions
        val removeItem = popup.menu.findItem(R.id.action_remove)
        val makeAdminItem = popup.menu.findItem(R.id.action_make_admin)
        val makeOwnerItem = popup.menu.findItem(R.id.action_make_owner)
        
        // Only admins can remove members
        removeItem.isVisible = isCurrentUserAdmin
        
        // Only admins can make others admins, and only if member is not already admin
        makeAdminItem.isVisible = isCurrentUserAdmin && !member.isAdmin
        
        // Only owner can transfer ownership
        makeOwnerItem.isVisible = isCurrentUserOwner && !member.isOwner
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_remove -> {
                    onRemoveMemberClicked(member.userId)
                    true
                }
                R.id.action_make_admin -> {
                    onMakeAdminClicked(member.userId)
                    true
                }
                R.id.action_make_owner -> {
                    onMakeOwnerClicked(member.userId)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    override fun getItemCount(): Int = members.size

    fun updateMembers(
        newMembers: List<MemberInfo>,
        currentUserId: String,
        isCurrentUserAdmin: Boolean,
        isCurrentUserOwner: Boolean
    ) {
        this.members = newMembers
        this.currentUserId = currentUserId
        this.isCurrentUserAdmin = isCurrentUserAdmin
        this.isCurrentUserOwner = isCurrentUserOwner
        notifyDataSetChanged()
    }

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberEmail: TextView = itemView.findViewById(R.id.member_email)
        val roleBadge: TextView = itemView.findViewById(R.id.role_badge)
        val optionsButton: ImageView = itemView.findViewById(R.id.member_options)
    }
} 