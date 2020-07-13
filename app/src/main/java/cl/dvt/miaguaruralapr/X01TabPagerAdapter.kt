package cl.dvt.miaguaruralapr

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

//Pager Adapter
/**FUENTE: https://medium.com/@eijaz/getting-started-with-tablayout-in-android-kotlin-bb7e21783761
 * https://github.com/kotlincodes/ViewPagerKotlinTutorial*/
class X01MainTabPagerAdapter (fm: FragmentManager, private var tabCount:Int) : FragmentPagerAdapter(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){
    override fun getItem(position: Int): Fragment {
        return when(position) {
            0   ->return F01ConsumptionFragment()
            1   ->return F02CostumerFragment()
            2   ->return F03TramoFragment()
            else->return F04ProduccionFragment()
        }
    }
    override fun getCount(): Int {
        return tabCount
    }
}