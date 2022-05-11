package com.example.hook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.core.PluginManager
import com.example.hook.databinding.FragmentFirstBinding

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

    private fun runPlugin(name: String) {
        try {
            PluginManager.runPlugin(requireContext(), name)

        } catch (e: RuntimeException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "启动失败,未安装$name", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnInstallPlugin.setOnClickListener {
            PluginManager.install(requireContext().getFileStreamPath("test.apk")?.path!!)
        }
        binding.btnDeletePlugin.setOnClickListener {
            PluginManager.unInstall("test")
        }
        binding.btnRunPlugin.setOnClickListener {
            runPlugin("test")
        }
//        binding.btnInstallPlugin2.setOnClickListener {
//            PluginManager.install(requireContext().getFileStreamPath("test2.apk")?.path!!)
//        }
//        binding.btnDeletePlugin2.setOnClickListener {
//            PluginManager.unInstall("test2")
//        }
//        binding.btnRunPlugin2.setOnClickListener {
//            runPlugin("test2")
//        }

    }

    fun getTitle(title: String): String = title


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}