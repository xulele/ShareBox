package com.newindia.sharebox.ui.dialog

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.newindia.sharebox.R
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.widget.Toolbar
import android.view.*
import android.content.res.TypedArray
import android.os.AsyncTask
import android.support.annotation.IntegerRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.newindia.sharebox.ui.activities.MainActivity
import com.newindia.sharebox.ui.fragments.PageFragment
import com.newindia.sharebox.utils.fileutils.FileUtil
import java.io.File


/**
 * Created by KerriGan on 2017/6/2.
 */
class FilePickDialog :BaseBottomSheetDialog{

    constructor(context: Context,activity: Activity? = null):super(context,activity){

    }

    private var mBehavior:BottomSheetBehavior<View>? =null

    private var mHeight=0

    private var mTabLayout:TabLayout? =null

    private var mViewPager:ViewPager? =null

    private var mTabItemHolders:MutableMap<String,TabItemHolder>? = mutableMapOf()

    private var mFileTypes: MutableMap<String, List<File>>? = mutableMapOf(Pair("Movie", listOf<File>()),
                    Pair("Music", listOf<File>()),
                    Pair("Photo", listOf<File>()),
                    Pair("Doc", listOf<File>()),
                    Pair("Apk", listOf<File>()),
                    Pair("Rar", listOf<File>()))

    private var mTaskTypes:MutableMap<String,LoadingFilesTask>? = mutableMapOf()

    override fun initializeDialog() {
        super.initializeDialog()
        context.setTheme(R.style.WhiteToolbar)
    }

    override fun onCreateView(): View? {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        var vg= layoutInflater.inflate(R.layout.dialog_file_pick,null)

        val display = ownerActivity.getWindowManager().getDefaultDisplay()
        val width = display.getWidth()
        val height = display.height/*getScreenHeight(ownerActivity)+getStatusBarHeight(context)*/
        mHeight=height

        vg.layoutParams=ViewGroup.LayoutParams(width,height)
        return vg
    }

    override fun onViewCreated(view: View?):Boolean {
        super.onViewCreated(view)
        mBehavior= BottomSheetBehavior.from(findViewById(android.support.design.R.id.design_bottom_sheet))
        mBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED

//        mBehavior?.skipCollapsed=true
        mBehavior?.peekHeight=mHeight*2/3

        initView(view as ViewGroup)
        return true
    }

    private fun initData(){
//        mTabItemHolders

    }

    protected fun initView(vg:ViewGroup){
        initData()

        var toolbar=vg.findViewById(R.id.toolbar) as Toolbar

        toolbar.setNavigationIcon(ColorDrawable(Color.TRANSPARENT))

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        toolbar.inflateMenu(R.menu.menu_file_pick)

        mBehavior?.setBottomSheetCallback(object :BottomSheetBehavior.BottomSheetCallback(){
            var mFitSystemWindow=false

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if(slideOffset==1.0f){
                    mFitSystemWindow=true
                }else{
                    if(mFitSystemWindow!=false){
                        toolbar.setNavigationIcon(ColorDrawable(Color.TRANSPARENT))
                        toolbar.fitsSystemWindows=false
                        toolbar.setPadding(toolbar.paddingLeft,0,toolbar.paddingRight,toolbar.paddingBottom)
                    }
                    mFitSystemWindow=false
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState==BottomSheetBehavior.STATE_EXPANDED){
                    val attrsArray = intArrayOf(android.R.attr.homeAsUpIndicator)
                    val typedArray = context.obtainStyledAttributes(attrsArray)
                    val dw = typedArray.getDrawable(0)
                    toolbar.setNavigationIcon(dw)
                    toolbar.fitsSystemWindows=true
                    toolbar.requestFitSystemWindows()

                    // don't forget the resource recycling
                    typedArray.recycle()
                    return
                }else if(newState==BottomSheetBehavior.STATE_HIDDEN){
                    dismiss()
                }

                if(!mFitSystemWindow){
                    toolbar.setNavigationIcon(ColorDrawable(Color.TRANSPARENT))
                    toolbar.fitsSystemWindows=false
                    toolbar.setPadding(toolbar.paddingLeft,0,toolbar.paddingRight,toolbar.paddingBottom)
                }
            }
        })

        mTabLayout=vg.findViewById(R.id.tab_layout) as TabLayout
        mViewPager=vg.findViewById(R.id.view_pager) as ViewPager

        mViewPager?.adapter=object :PagerAdapter(){


            override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
                return view==`object`
            }

            override fun getCount(): Int {
                return mFileTypes?.size ?:0
            }

            override fun getPageTitle(position: Int): CharSequence {
                return mFileTypes?.keys?.elementAt(position)!!
            }

            override fun instantiateItem(container: ViewGroup?, position: Int): Any {
                var vg=layoutInflater.inflate(R.layout.layout_main_activity_data,container,false)
                container?.addView(vg)

                var title=getPageTitle(position).toString()

                if(mTaskTypes?.get(title)==null){
                    var task=LoadingFilesTask(context,string2MediaFileType(title)!!)
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    mTaskTypes?.put(title,task)
                }
                return vg
            }

            override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
                var title=getPageTitle(position).toString()
                if(mTaskTypes?.get(title)!=null){
                    var task=mTaskTypes?.get(title)
                    task?.cancel(true)
                    mTaskTypes?.remove(title)
                }
                container?.removeView(`object` as View)
            }
        }

        mTabLayout?.setupWithViewPager(mViewPager)
    }

     protected inner class LoadingFilesTask:AsyncTask<List<File>?,Void,List<File>?>{
        private val TAG="LoadingFilesTask"

        private var mType:FileUtil.MediaFileType? =null

        private var mContext: Context? =null
        constructor(context: Context,type: FileUtil.MediaFileType):super(){
            mType=type
            mContext=context
        }

        override fun doInBackground(vararg params: List<File>?): List<File>? {
            Log.e(TAG,mediaFileType2String(mType!!)+" task begin")

            var list:List<File>? = null

            when(mType){
                FileUtil.MediaFileType.MOVIE->{
                    list=FileUtil.getAllMediaFile(mContext!!,null)
                    mFileTypes?.put("Movie",list)
                }
                FileUtil.MediaFileType.MP3->{
                    list=FileUtil.getAllMusicFile(mContext!!,null)
                    mFileTypes?.put("Music",list)
                }
                FileUtil.MediaFileType.IMG->{
                    list=FileUtil.getAllImageFile(mContext!!,null)
                    mFileTypes?.put("Photo",list)
                }
                FileUtil.MediaFileType.DOC->{
                    list=FileUtil.getAllDocFile(mContext!!,null)
                    mFileTypes?.put("Doc",list)
                }
                FileUtil.MediaFileType.APP->{
                    list=FileUtil.getAllApkFile(mContext!!,null)
                    mFileTypes?.put("Apk",list)
                }
                FileUtil.MediaFileType.RAR->{
                    list=FileUtil.getAllRarFile(mContext!!,null)
                    mFileTypes?.put("Rar",list)
                }
            }

            if(!isCancelled)
                Log.e(TAG,mediaFileType2String(mType!!)+" task finished")
            return list
        }

        override fun onPostExecute(result: List<File>?) {
            super.onPostExecute(result)
        }

        override fun onCancelled(result: List<File>?) {
            super.onCancelled(result)
            Log.e(TAG,mediaFileType2String(mType!!)+" task cancelled")
        }
    }

    fun string2MediaFileType(str:String):FileUtil.MediaFileType?{
        var ret:FileUtil.MediaFileType?=null
        when(str){
            "Movie"->{
                ret=FileUtil.MediaFileType.MOVIE
            }
            "Music"->{
                ret=FileUtil.MediaFileType.MP3
            }
            "Photo"->{
                ret=FileUtil.MediaFileType.IMG
            }
            "Doc"->{
                ret=FileUtil.MediaFileType.DOC
            }
            "Apk"->{
                ret=FileUtil.MediaFileType.APP
            }
            "Rar"->{
                ret=FileUtil.MediaFileType.RAR
            }
        }
        return ret
    }

    fun mediaFileType2String(type:FileUtil.MediaFileType):String?{
        var ret:String?=null
        when(type){
            FileUtil.MediaFileType.MOVIE->{
                ret="Movie"
            }
            FileUtil.MediaFileType.MP3->{
                ret="Music"
            }
            FileUtil.MediaFileType.IMG->{
                ret="Photo"
            }
            FileUtil.MediaFileType.DOC->{
                ret="Doc"
            }
            FileUtil.MediaFileType.APP->{
                ret="Apk"
            }
            FileUtil.MediaFileType.RAR->{
                ret="Rar"
            }
        }
        return ret
    }

    protected class TabItemHolder{
        var title:String? =null
        var type:FileUtil.MediaFileType? =null
        var task:LoadingFilesTask? =null
        var fileList:List<File>? =null
    }
}