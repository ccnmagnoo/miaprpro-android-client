package cl.dvt.miaguaruralapr.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cl.dvt.miaguaruralapr.SplashActivity.Companion.user
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Tramo
import cl.dvt.miaguaruralapr.adapters.TramoItemAdapter
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_tramo.*
import kotlinx.android.synthetic.main.dialog_update_tramo.view.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class TramoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tramo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //fetchTramo()
        Tramo().fetchTramo(requireContext(),tramo_recyclerView_tramo,consumption_chart_tramo )

        //on item click
        addTramo_floatingButton_tramo.setOnClickListener {
            Tramo().createDialog(requireContext(), user!!.tramoList)
        }



    }




}
