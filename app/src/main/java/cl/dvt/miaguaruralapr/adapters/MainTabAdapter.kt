package cl.dvt.miaguaruralapr.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import cl.dvt.miaguaruralapr.fragments.ConsumptionFragment
import cl.dvt.miaguaruralapr.fragments.CostumerFragment
import cl.dvt.miaguaruralapr.fragments.ProduccionFragment
import cl.dvt.miaguaruralapr.fragments.TramoFragment

//Pager Adapter
/**FUENTE: https://medium.com/@eijaz/getting-started-with-tablayout-in-android-kotlin-bb7e21783761
 * https://github.com/kotlincodes/ViewPagerKotlinTutorial*/
class MainTabAdapter (fm: FragmentManager, private var tabCount:Int) : FragmentPagerAdapter(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){
    override fun getItem(position: Int): Fragment {
        return when(position) {
            0   -> ConsumptionFragment()
            1   -> CostumerFragment()
            2   -> TramoFragment()
            else-> ProduccionFragment()
        }
    }
    override fun getCount(): Int {
        return tabCount
    }
}