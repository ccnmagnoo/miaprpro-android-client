package cl.dvt.miaguaruralapr

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.SearchView
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.section_toolbar_main.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    companion object{
        var QUERY_KEY:String? = "QUERY_WORD"
        var block_key:Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verifyUserIsLoggedIn() /* verificando usuario si no ir a login */

        configureTabLayout()   /* instando pestañas */

        search_searchView_toolbar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("Search", "query word : $query")
                val intent = Intent(this@MainActivity, A04SearchActivity::class.java)
                intent.putExtra(QUERY_KEY, query)
                startActivity(intent)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

    }

    //F01. FUNCIÓN verificar Loggin;
    private fun verifyUserIsLoggedIn(){
        val uid = FirebaseAuth.getInstance().uid /**buscar la uid actual*/
        Log.d("CurrentUser", "UID : $uid")
        val ref = FirebaseFirestore.getInstance().collection("userApr").document("$uid")
        if (uid==null){
            val intent = Intent(this, A02LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

    }/**fin verifyUserIsLoggedIn*/

    //F02. FUNCIÓN TabLayout
    private fun configureTabLayout(){
        tabLayout_main.addTab(tabLayout_main.newTab().setText("Consumo").setIcon(R.drawable.ic_ico_clock_24dp))
        tabLayout_main.addTab(tabLayout_main.newTab().setText("Usuarios").setIcon(R.drawable.ic_ico_person_20dp))
        tabLayout_main.addTab(tabLayout_main.newTab().setText("Tramos").setIcon(R.drawable.ic_ico_coin_20dp))
        tabLayout_main.addTab(tabLayout_main.newTab().setText("Producción").setIcon(R.drawable.ic_ico_production_20dp))
        val fragmentAdapter = X01MainTabPagerAdapter(
            supportFragmentManager,
            tabLayout_main.tabCount
        )
        viewPager_main.adapter = fragmentAdapter
        viewPager_main.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout_main))
        tabLayout_main.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?){
                viewPager_main.currentItem = tab!!.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )
    }

    //F03. Acciones en selección de items de ToolBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val x = item.itemId
        Log.d("Menu", "item menu id selected : $x")
        when (item.itemId){
            /*ir a pantalla de login*/
            R.id.logOut_menuItem_main ->{
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, A02LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }


}

