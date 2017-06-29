package com.opar.mobile.money;

/**
 * Created by wangbo on 17/6/29.
 */

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

public class RedPacketService2 extends AccessibilityService {

    private String TAG = getClass().getSimpleName();
    private static final String WECHAT_OPEN_CH = "拆红包";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";

    private boolean mCycle;
    private boolean isClicked;
    private boolean mLuckyMoneyReceived;
    private boolean mLuckyMoneyPicked;

    private long lastFetchedTime;
    private String lastFetchedHongBaoId;
    private int MAX_CACHE_TOLERANCE = 20;

    private AccessibilityNodeInfo rootNodeInfo;
    private List<AccessibilityNodeInfo> mReceiveNodeList;
    private List<AccessibilityNodeInfo> mUnpackNodeList;

    /**
     * 必须重写的方法：此方法用了接受系统发来的event。在你注册的event发生是被调用。在整个生命周期会被调用多次。
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG,"事件 －－－－》 startGrapHongBao " + event);
        int eventType = event.getEventType();
        switch (eventType) {
            //通知栏来信息，判断是否含有微信红包字样，是的话跳转
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                if(!mCycle){
                    List<CharSequence> texts = event.getText();
                    if(!texts.isEmpty()){
                        for(CharSequence t :texts){
                            String text = String.valueOf(t);
                            Log.d(TAG,"事件 －－－－》 notifychanged "+text);
                            if(text.contains(WECHAT_NOTIFICATION_TIP)){
                                isClicked = false;
                                openNotify(event);
                            }
                        }
                    }
                }

                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if(!mCycle){
                    startGrapHongBao(event);
                }
                break;
        }
    }

    private void startGrapHongBao(AccessibilityEvent event){
        Log.d(TAG,"事件 －－－－》 startGrapHongBao "+event);
        this.rootNodeInfo = event.getSource();
        if(rootNodeInfo == null) return;
        mReceiveNodeList = null;
        mUnpackNodeList = null;
        if(mLuckyMoneyReceived && !mLuckyMoneyPicked && (mReceiveNodeList != null)){
            int size = mReceiveNodeList.size();
            Log.d(TAG,"事件 －－－－》 performAction 节点数目 " + mReceiveNodeList.size());
            if(size > 0){
                Log.d(TAG,"事件 －－－－》 start 已经接收到红包并且还没有戳开 ");
                AccessibilityNodeInfo cellNode = mReceiveNodeList.get(size - 1);
                String id = getHongbaoText(cellNode);
                long now = System.currentTimeMillis();
                if(this.shouldReturn(id,now - lastFetchedTime))
                    return;
                mCycle = true;
                lastFetchedHongBaoId = id;
                lastFetchedTime = now;
                cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG,"事件 －－－－》 end 已经接收到红包并且还没有戳开 click");
                mLuckyMoneyReceived = false;
                mLuckyMoneyPicked = true;
            }
        }

    }

    /**
     * 将节点对象的id和红包上的内容合并
     * 用于表示一个唯一的红包
     *
     * @param node 任意对象
     * @return 红包标识字符串
     */
    private String getHongbaoText(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npe) {
            return null;
        }
        Log.d(TAG, "事件----> start getHongbaoText（） " + content);
        return content;
    }

    /** 打开通知栏消息*/
    private void openNotify(AccessibilityEvent event){
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)){
            return;
        }
        //将微信的通知栏消息打开
        Notification notification = (Notification)event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        Log.d(TAG, "事件----> 打开通知栏消息 " + event);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void checkNodeInfo(){
        Log.d(TAG, "事件----> start checkNodeInfo");
        if(this.rootNodeInfo == null)
            return;
    }

    private boolean shouldReturn(String id ,long duration){
        if(id == null)
            return true;
        if(duration < MAX_CACHE_TOLERANCE && id.equals(lastFetchedHongBaoId)){
            return true;
        }
        return false;
    }

    /**
     * 必须重写的方法：系统要中断此service返回的响应时会调用。在整个生命周期会被调用多次。
     */
    @Override
    public void onInterrupt() {
        Toast.makeText(this, "我快被终结了啊-----", Toast.LENGTH_SHORT).show();
    }

    /**
     * 服务已连接
     */
    @Override
    protected void onServiceConnected() {
        Toast.makeText(this, "抢红包服务开启", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
    }

    /**
     * 服务已断开
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "抢红包服务已被关闭", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}