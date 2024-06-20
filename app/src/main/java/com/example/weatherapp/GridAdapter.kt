package com.example.weatherapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class GridAdapter(private val context: Context, private var dataList: List<GridItems>) : BaseAdapter() {

    override fun getCount(): Int {
        return dataList.size
    }

    override fun getItem(position: Int): Any {
        return dataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val viewHolder: ViewHolder

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.grid_items, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        viewHolder.textView.text = dataList[position].tempValue
        viewHolder.textView2.text = dataList[position].tempType
        viewHolder.img.setImageResource(dataList[position].imageId)

        return view!!
    }

    fun updateData(newDataList: List<GridItems>) {
         dataList = newDataList
        notifyDataSetChanged()
    }

    private class ViewHolder(view: View) {
        val textView: TextView = view.findViewById(R.id.tempValue)
        val textView2: TextView = view.findViewById(R.id.tempType)
        val img: ImageView = view.findViewById(R.id.tempImage)
    }
}
