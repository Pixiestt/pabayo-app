package com.example.capstone2.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.capstone2.R
import com.example.capstone2.data.models.MillingType

class MillingDetailsFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
    private var sackCount = 1
    private lateinit var tvSackCount: TextView
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_milling_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        radioGroup = view.findViewById(R.id.radioGroupMilling)
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        tvSackCount = view.findViewById(R.id.tvSackCount)
        btnPlus = view.findViewById(R.id.btnPlus)
        btnMinus = view.findViewById(R.id.btnMinus)

        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "1/4"

        // Prefill from wizard data if editing
        val wizard = activity.getWizardData()
        sackCount = wizard.sackCount
        tvSackCount.text = sackCount.toString()

        // Since the layout no longer contains radio options, enable Next so the user can proceed
        btnNext.isEnabled = true

        // Set up radio button listeners — keep minimal and avoid referencing removed IDs
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            // Enable the Next button when an option is selected (still safe if radios are later added)
            btnNext.isEnabled = true

            // Try to detect the selected radio and map to MillingType if applicable.
            val selectedRadio = view.findViewById<RadioButton>(checkedId)
            when (selectedRadio?.text?.toString()) {
                // If you later add a radio with text matching these strings, map accordingly.
                getString(R.string.milling_for_fee) -> activity.getWizardData().millingType = MillingType.MILLING_FOR_FEE
                getString(R.string.sr_feeds_conversion) -> activity.getWizardData().millingType = MillingType.MILLING_FOR_RICE_CONVERSION
                else -> {
                    // No mapping; leave millingType unchanged or set to null depending on desired behavior.
                }
            }
        }
        
        // Removed click binding to deleted CardView (cardMillingForFee) to avoid referencing missing view IDs.

        btnPlus.setOnClickListener {
            sackCount++
            tvSackCount.text = sackCount.toString()
        }
        btnMinus.setOnClickListener {
            if (sackCount > 1) {
                sackCount--
                tvSackCount.text = sackCount.toString()
            }
        }

        btnNext.setOnClickListener {
            // Allow proceeding even if there is no radio selected (layout currently has no radios)
            activity.getWizardData().sackCount = sackCount
            activity.goToNextStep()
        }
    }
}
