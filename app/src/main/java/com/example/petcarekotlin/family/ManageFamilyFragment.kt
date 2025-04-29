package com.example.petcarekotlin.family

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.util.DisplayMetrics
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.core.AppPageFragment
import com.example.petcarekotlin.R
import com.example.petcarekotlin.models.FamilyModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.petcarekotlin.util.QRCodeGenerator

class ManageFamilyFragment : Fragment() {
    
    private var inviteQrDialog: BottomSheetDialog? = null
    private var createFamilyDialog: BottomSheetDialog? = null
    private var joinFamilyDialog: BottomSheetDialog? = null
    
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var noFamilyView: View
    private lateinit var familyContentView: View
    private lateinit var createFamilyButton: Button
    private lateinit var joinFamilyButton: Button
    private var currentFamilyId: String? = null
    
    private val repository = FamilyRepository()
    private lateinit var membersAdapter: FamilyMembersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_manage_family, container, false)
        
        // Initialize views
        membersRecyclerView = view.findViewById(R.id.family_members_list)
        noFamilyView = view.findViewById(R.id.no_family_layout)
        familyContentView = view.findViewById(R.id.family_content)
        createFamilyButton = view.findViewById(R.id.create_family_button)
        joinFamilyButton = view.findViewById(R.id.join_family_button)
        
        // Set up back button
        view.findViewById<ImageView>(R.id.back_button)?.setOnClickListener {
            // Return to UserProfileFragment in sidebar
            (parentFragment as? AppPageFragment)?.showUserProfileInSidebar()
        }
        
        // Set up close button
        view.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
            // Close the drawer
            (parentFragment as? AppPageFragment)?.closeDrawer()
        }
        
        // Set up create family button
        createFamilyButton.setOnClickListener {
            showCreateFamilyDialog()
        }
        
        // Set up join family button
        joinFamilyButton.setOnClickListener {
            showJoinFamilyDialog()
        }
        
        // Set up invite new member button
        view.findViewById<Button>(R.id.invite_new_member_button)?.setOnClickListener {
            currentFamilyId?.let {
                showInviteQrCodeSheet(it)
            }
        }
        
        // Set up leave family button
        view.findViewById<Button>(R.id.leave_family_button)?.setOnClickListener {
            leaveCurrentFamily()
        }
        
        // Setup recycler view for members
        setupMembersRecyclerView()
        
        // Set width to 4/5 of the screen
        setLayoutWidth(view)
        
        // Load user's family data
        loadFamilyData()
        
        return view
    }
    
    private fun setupMembersRecyclerView() {
        membersAdapter = FamilyMembersAdapter(
            onMakeAdminClicked = { memberId -> makeAdmin(memberId) },
            onRemoveMemberClicked = { memberId -> removeMember(memberId) },
            onMakeOwnerClicked = { memberId -> transferOwnership(memberId) }
        )
        
        membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        membersRecyclerView.adapter = membersAdapter
    }
    
    private fun loadFamilyData() {
        // Get current user ID from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val userId = sharedPrefs.getString("CURRENT_USER_ID", null)
        
        if (userId == null) {
            showNoFamilyState()
            return
        }
        
        // Show loading state
        showLoadingState()
        
        // Load user's families
        lifecycleScope.launch {
            try {
                val families = repository.getUserFamilies(userId)
                
                if (families.isEmpty()) {
                    showNoFamilyState()
                    return@launch
                }
                
                // Use the first family for now (could add family selection later)
                val family = families.first()
                displayFamilyInfo(family)
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading family: ${e.message}", Toast.LENGTH_SHORT).show()
                showNoFamilyState()
            }
        }
    }
    
    private fun displayFamilyInfo(family: FamilyModel) {
        currentFamilyId = family.familyId
        
        // Show family content
        noFamilyView.visibility = View.GONE
        familyContentView.visibility = View.VISIBLE
        
        // Load family members
        loadFamilyMembers(family)
    }
    
    private fun loadFamilyMembers(family: FamilyModel) {
        // Get current user ID
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val currentUserId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: return
        
        lifecycleScope.launch {
            try {
                // Get member info for each member ID
                val membersList = mutableListOf<MemberInfo>()
                
                for (memberId in family.members) {
                    // For each member ID, fetch their user data
                    try {
                        val userDoc = repository.getUserById(memberId)
                        val isAdmin = family.admins.contains(memberId)
                        val isOwner = family.createdBy == memberId
                        val fullName = userDoc.getString("fullName") ?: "Unknown User"
                        val email = userDoc.getString("email") ?: ""
                        
                        membersList.add(MemberInfo(
                            userId = memberId,
                            username = fullName,
                            email = email,
                            isAdmin = isAdmin,
                            isOwner = isOwner,
                            isCurrentUser = memberId == currentUserId
                        ))
                    } catch (e: Exception) {
                        // If we can't get user data, still add a placeholder
                        membersList.add(MemberInfo(
                            userId = memberId,
                            username = "User $memberId",
                            isAdmin = family.admins.contains(memberId),
                            isOwner = family.createdBy == memberId,
                            isCurrentUser = memberId == currentUserId
                        ))
                    }
                }
                
                // Update adapter with members
                membersAdapter.updateMembers(membersList, 
                    currentUserId = currentUserId, 
                    isCurrentUserAdmin = family.admins.contains(currentUserId),
                    isCurrentUserOwner = family.createdBy == currentUserId
                )
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading members: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun makeAdmin(memberId: String) {
        val familyId = currentFamilyId ?: return
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val currentUserId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: return
        
        lifecycleScope.launch {
            try {
                repository.makeAdmin(familyId, memberId, currentUserId)
                Toast.makeText(requireContext(), "Member promoted to admin", Toast.LENGTH_SHORT).show()
                // Reload family to see changes
                loadFamilyData()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun removeMember(memberId: String) {
        val familyId = currentFamilyId ?: return
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val currentUserId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: return
        
        lifecycleScope.launch {
            try {
                repository.removeMember(familyId, memberId, currentUserId)
                Toast.makeText(requireContext(), "Member removed from family", Toast.LENGTH_SHORT).show()
                // Reload family to see changes
                loadFamilyData()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun transferOwnership(memberId: String) {
        val familyId = currentFamilyId ?: return
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val currentUserId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: return
        
        lifecycleScope.launch {
            try {
                repository.transferOwnership(familyId, memberId, currentUserId)
                Toast.makeText(requireContext(), "Ownership transferred", Toast.LENGTH_SHORT).show()
                // Reload family to see changes
                loadFamilyData()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun leaveCurrentFamily() {
        val familyId = currentFamilyId ?: return
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
        val currentUserId = sharedPrefs.getString("CURRENT_USER_ID", null) ?: return
        
        lifecycleScope.launch {
            try {
                repository.leaveFamily(familyId, currentUserId)
                Toast.makeText(requireContext(), "You left the family", Toast.LENGTH_SHORT).show()
                // Reset UI
                showNoFamilyState()
                currentFamilyId = null
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showCreateFamilyDialog() {
        // Create bottom sheet dialog
        createFamilyDialog = BottomSheetDialog(requireContext())
        
        // Inflate create family layout
        val createFamilyView = layoutInflater.inflate(R.layout.dialog_create_family, null)
        
        // Set the content view
        createFamilyDialog?.setContentView(createFamilyView)
        
        // Set up create button
        createFamilyView.findViewById<Button>(R.id.create_family_confirm_button)?.setOnClickListener {
            val familyNameInput = createFamilyView.findViewById<TextInputEditText>(R.id.family_name_input)
            val familyName = familyNameInput.text.toString().trim()
            
            if (familyName.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a family name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Get current user ID
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
            val userId = sharedPrefs.getString("CURRENT_USER_ID", null)
            
            if (userId == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                createFamilyDialog?.dismiss()
                return@setOnClickListener
            }
            
            // Create the family
            lifecycleScope.launch {
                try {
                    // Show loading
                    val createButton = createFamilyView.findViewById<Button>(R.id.create_family_confirm_button)
                    createButton.isEnabled = false
                    createButton.text = "Creating..."
                    
                    val family = repository.createFamily(familyName, userId)
                    
                    Toast.makeText(requireContext(), "Family created successfully", Toast.LENGTH_SHORT).show()
                    createFamilyDialog?.dismiss()
                    
                    // Display the newly created family
                    displayFamilyInfo(family)
                    
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error creating family: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Reset button
                    val createButton = createFamilyView.findViewById<Button>(R.id.create_family_confirm_button)
                    createButton.isEnabled = true
                    createButton.text = "Create Family"
                }
            }
        }
        
        // Set up cancel button
        createFamilyView.findViewById<Button>(R.id.cancel_button)?.setOnClickListener {
            createFamilyDialog?.dismiss()
        }
        
        // Show the dialog
        createFamilyDialog?.show()
    }
    
    private fun showJoinFamilyDialog() {
        // Create bottom sheet dialog
        joinFamilyDialog = BottomSheetDialog(requireContext())
        
        // Inflate join family layout
        val joinFamilyView = layoutInflater.inflate(R.layout.dialog_join_family, null)
        
        // Set the content view
        joinFamilyDialog?.setContentView(joinFamilyView)
        
        // Set up join button
        joinFamilyView.findViewById<Button>(R.id.btnJoin)?.setOnClickListener {
            val inviteCodeInput = joinFamilyView.findViewById<TextInputEditText>(R.id.etFamilyCode)
            val inviteCode = inviteCodeInput.text.toString().trim()
            
            if (inviteCode.isBlank()) {
                Toast.makeText(requireContext(), "Please enter an invite code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Get current user ID
            val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", 0)
            val userId = sharedPrefs.getString("CURRENT_USER_ID", null)
            
            if (userId == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                joinFamilyDialog?.dismiss()
                return@setOnClickListener
            }
            
            // Join the family
            lifecycleScope.launch {
                try {
                    // Show loading
                    val joinButton = joinFamilyView.findViewById<Button>(R.id.btnJoin)
                    joinButton.isEnabled = false
                    joinButton.text = "Joining..."
                    
                    // Get family by invite code directly without using repository
                    val familiesCollection = FirebaseFirestore.getInstance().collection("families")
                    val querySnapshot = familiesCollection.whereEqualTo("inviteCode", inviteCode).get().await()
                    
                    if (querySnapshot.isEmpty) {
                        throw Exception("Invalid invite code")
                    }
                    
                    val familyDoc = querySnapshot.documents[0]
                    val familyId = familyDoc.id
                    
                    // Add user to family members
                    familiesCollection.document(familyId)
                        .update(
                            "members", com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                            "updatedAt", System.currentTimeMillis()
                        ).await()
                    
                    // Add family to user's families
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("families", com.google.firebase.firestore.FieldValue.arrayUnion(familyId))
                        .await()
                    
                    // Get updated family data
                    val updatedFamilyDoc = familiesCollection.document(familyId).get().await()
                    val family = updatedFamilyDoc.toObject(FamilyModel::class.java)
                        ?: throw Exception("Error retrieving updated family data")
                    
                    Toast.makeText(requireContext(), "Joined family successfully", Toast.LENGTH_SHORT).show()
                    joinFamilyDialog?.dismiss()
                    
                    // Display the joined family
                    displayFamilyInfo(family)
                    
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error joining family: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Reset button
                    val joinButton = joinFamilyView.findViewById<Button>(R.id.btnJoin)
                    joinButton.isEnabled = true
                    joinButton.text = "Join Family"
                }
            }
        }
        
        // Set up cancel button
        joinFamilyView.findViewById<Button>(R.id.btnCancel)?.setOnClickListener {
            joinFamilyDialog?.dismiss()
        }
        
        // Show the dialog
        joinFamilyDialog?.show()
    }
    
    private fun showInviteQrCodeSheet(familyId: String) {
        // Create bottom sheet dialog
        inviteQrDialog = BottomSheetDialog(requireContext())
        
        // Inflate QR code layout
        val inviteView = layoutInflater.inflate(R.layout.layout_invite_qr_code, null)
        
        // Get invite code for the family
        lifecycleScope.launch {
            try {
                // Get family document
                val familyDoc = FirebaseFirestore.getInstance().collection("families").document(familyId).get().await()
                val family = familyDoc.toObject(FamilyModel::class.java) ?: throw Exception("Could not retrieve family data")
                
                // Display invite code
                val inviteCodeText = inviteView.findViewById<TextView>(R.id.invite_code)
                inviteCodeText.text = family.inviteCode
                
                // Generate and display QR code
                val qrCodeImage = inviteView.findViewById<ImageView>(R.id.qr_code_image)
                val qrContent = "petcare:family:${family.inviteCode}" // Use a custom URI scheme for the app
                val qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 512, 512)
                qrCodeImage.setImageBitmap(qrBitmap)
                
                // Set copy button action
                inviteView.findViewById<FloatingActionButton>(R.id.copy_code_button)?.setOnClickListener {
                    // Copy code to clipboard
                    val clipboard = requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Invite Code", family.inviteCode)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(requireContext(), "Invite code copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading invite code: ${e.message}", Toast.LENGTH_SHORT).show()
                inviteQrDialog?.dismiss()
            }
        }
        
        // Set the content view
        inviteQrDialog?.setContentView(inviteView)
        
        // Set up close button
        inviteView.findViewById<Button>(R.id.close_invite)?.setOnClickListener {
            inviteQrDialog?.dismiss()
        }
        
        // Set height to 3/4 of screen
        val displayMetrics = DisplayMetrics()
        val windowManager = requireActivity().windowManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val screenHeight = bounds.height()
            val bottomSheetHeight = (screenHeight * 0.75).toInt()
            
            inviteView.findViewById<View>(R.id.close_invite)?.post {
                val bottomSheet = inviteQrDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val layoutParams = bottomSheet?.layoutParams
                layoutParams?.height = bottomSheetHeight
                bottomSheet?.layoutParams = layoutParams
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenHeight = displayMetrics.heightPixels
            val bottomSheetHeight = (screenHeight * 0.75).toInt()
            
            inviteView.findViewById<View>(R.id.close_invite)?.post {
                val bottomSheet = inviteQrDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val layoutParams = bottomSheet?.layoutParams
                layoutParams?.height = bottomSheetHeight
                bottomSheet?.layoutParams = layoutParams
            }
        }
        
        // Show the dialog
        inviteQrDialog?.show()
    }
    
    private fun setLayoutWidth(view: View) {
        // Get screen width
        val displayMetrics = DisplayMetrics()
        val windowManager = requireActivity().windowManager
        
        view.post {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                val screenWidth = bounds.width()
                val params = view.layoutParams
                params.width = (screenWidth * 4) / 5
                view.layoutParams = params
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val params = view.layoutParams
                params.width = (screenWidth * 4) / 5
                view.layoutParams = params
            }
        }
    }
    
    private fun showNoFamilyState() {
        noFamilyView.visibility = View.VISIBLE
        familyContentView.visibility = View.GONE
    }
    
    private fun showLoadingState() {
        noFamilyView.visibility = View.GONE
        familyContentView.visibility = View.GONE
        // You can add a progress indicator here if needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Dismiss dialogs if they're showing when fragment is destroyed
        if (inviteQrDialog?.isShowing == true) {
            inviteQrDialog?.dismiss()
        }
        if (createFamilyDialog?.isShowing == true) {
            createFamilyDialog?.dismiss()
        }
        if (joinFamilyDialog?.isShowing == true) {
            joinFamilyDialog?.dismiss()
        }
    }

    /**
     * Load family data by the current user's username
     */
    private fun loadFamilyByUsername(username: String) {
        lifecycleScope.launch {
            try {
                // First, find the user document by username
                val usersCollection = FirebaseFirestore.getInstance().collection("users")
                val querySnapshot = usersCollection.whereEqualTo("username", username).get().await()
                
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    showNoFamilyState()
                    return@launch
                }
                
                // Get the user document
                val userDoc = querySnapshot.documents.first()
                val userId = userDoc.id
                
                // Check if user has a familyId
                val familyId = userDoc.getString("familyId")
                if (familyId.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "User doesn't belong to any family", Toast.LENGTH_SHORT).show()
                    showNoFamilyState()
                    return@launch
                }
                
                // Get the family document
                val familiesCollection = FirebaseFirestore.getInstance().collection("families")
                val familyDoc = familiesCollection.document(familyId).get().await()
                
                if (!familyDoc.exists()) {
                    Toast.makeText(requireContext(), "Family not found", Toast.LENGTH_SHORT).show()
                    showNoFamilyState()
                    return@launch
                }
                
                // Get members array from family document
                val members = familyDoc.get("members") as? List<String> ?: emptyList()
                
                // Fetch member details
                val membersList = mutableListOf<MemberInfo>()
                for (memberId in members) {
                    try {
                        val memberDoc = FirebaseFirestore.getInstance().collection("users").document(memberId).get().await()
                        val username = memberDoc.getString("username") ?: "Unknown User"
                        val email = memberDoc.getString("email") ?: ""
                        val isAdmin = (familyDoc.get("admins") as? List<String>)?.contains(memberId) ?: false
                        val isOwner = familyDoc.getString("createdBy") == memberId
                        
                        membersList.add(MemberInfo(
                            userId = memberId,
                            username = username,
                            email = email,
                            isAdmin = isAdmin,
                            isOwner = isOwner,
                            isCurrentUser = memberId == userId
                        ))
                    } catch (e: Exception) {
                        // Handle exception
                        continue
                    }
                }
                
                // Update the adapter with members
                membersAdapter.updateMembers(
                    newMembers = membersList,
                    currentUserId = userId,
                    isCurrentUserAdmin = (familyDoc.get("admins") as? List<String>)?.contains(userId) ?: false,
                    isCurrentUserOwner = familyDoc.getString("createdBy") == userId
                )
                
                // Display family info
                val familyName = familyDoc.getString("name") ?: "My Family"
                currentFamilyId = familyId
                
                // Show family content
                noFamilyView.visibility = View.GONE
                familyContentView.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading family: ${e.message}", Toast.LENGTH_SHORT).show()
                showNoFamilyState()
            }
        }
    }
} 