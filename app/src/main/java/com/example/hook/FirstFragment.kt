package com.example.hook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.hook.databinding.FragmentFirstBinding
import top.canyie.pine.Pine
import top.canyie.pine.callback.MethodHook

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val method = FirstFragment::class.java.getMethod("getTitle", String::class.java)
        val unhook = Pine.hook(method, object : MethodHook() {
            override fun beforeCall(callFrame: Pine.CallFrame?) {
                callFrame!!.args[0] = "hook success"
            }
        })
        binding.buttonFirst.setOnClickListener {

//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            val name = getTitle("hook failed")

            Toast.makeText(activity, name, Toast.LENGTH_SHORT).show()
        }
    }
    fun getTitle(title: String): String = title


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}