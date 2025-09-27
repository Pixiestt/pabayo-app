class MillingDetailsFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
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
        
        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "1/5"
        
        // Set up radio button listeners
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbMillingForFee -> {
                    activity.getWizardData().millingType = MillingType.MILLING_FOR_FEE
                }
                // Remove these lines
                R.id.rbMillingForRiceConversion -> {
                    activity.getWizardData().millingType = MillingType.MILLING_FOR_RICE_CONVERSION
                }
                
                // And remove this
                val cardMillingForRiceConversion = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardMillingForRiceConversion)
                
                // And remove this
                cardMillingForRiceConversion?.setOnClickListener {
                    radioGroup.check(R.id.rbMillingForRiceConversion)
                }
            }
            // Enable the next button when an option is selected
            btnNext.isEnabled = true
        }
        
        // Set up CardView click listener
        val cardMillingForFee = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardMillingForFee)
        
        cardMillingForFee?.setOnClickListener {
            radioGroup.check(R.id.rbMillingForFee)
        }
        
        // Initially disable the next button
        btnNext.isEnabled = false
        
        btnNext.setOnClickListener {
            if (radioGroup.checkedRadioButtonId == -1) {
                // Show error message if no option selected
                Toast.makeText(context, "Please select a milling type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activity.goToNextStep()
        }
    }
}