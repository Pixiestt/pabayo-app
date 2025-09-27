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
import com.example.capstone2.data.models.RequestWizardData
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
        tvStepProgress.text = "1/5"
        
        // Initially disable the Next button
        btnNext.isEnabled = false
        
        // Set up radio button listeners
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            // Enable the Next button when an option is selected
            btnNext.isEnabled = true
            
            val selectedRadio = view.findViewById<RadioButton>(checkedId)
            when (selectedRadio?.id) {
                R.id.rbMillingForFee -> {
                    activity.getWizardData().millingType = MillingType.MILLING_FOR_FEE
                }
            }
        }
        
        // Set up CardView click listener
        val cardMillingForFee = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardMillingForFee)
        
        cardMillingForFee?.setOnClickListener {
            radioGroup.check(R.id.rbMillingForFee)
        }
        
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
            if (radioGroup.checkedRadioButtonId != -1) {
                activity.getWizardData().sackCount = sackCount
                activity.goToNextStep()
            }
        }
    }
}
