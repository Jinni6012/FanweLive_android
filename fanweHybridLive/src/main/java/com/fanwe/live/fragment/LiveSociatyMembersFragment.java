package com.fanwe.live.fragment;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;

import com.fanwe.hybrid.fragment.BaseFragment;
import com.fanwe.hybrid.http.AppRequestCallback;
import com.fanwe.library.adapter.http.model.SDResponse;
import com.fanwe.library.dialog.SDDialogConfirm;
import com.fanwe.library.dialog.SDDialogCustom;
import com.fanwe.library.listener.SDItemClickCallback;
import com.fanwe.library.utils.SDToast;
import com.fanwe.library.utils.SDViewUtil;
import com.fanwe.library.view.SDTabUnderline;
import com.fanwe.live.R;
import com.fanwe.live.activity.LiveUserHomeActivity;
import com.fanwe.live.adapter.LiveSociatyMembersAdapter;
import com.fanwe.live.common.CommonInterface;
import com.fanwe.live.dao.UserModelDao;
import com.fanwe.live.model.App_sociaty_joinActModel;
import com.fanwe.live.model.App_sociaty_user_listActModel;
import com.fanwe.live.model.PageModel;
import com.fanwe.live.model.UserModel;
import com.fanwe.live.view.SDProgressPullToRefreshListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;

import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会成员列表
 * Created by Administrator on 2016/9/26.
 */

public class LiveSociatyMembersFragment extends BaseFragment
{
    @ViewInject(R.id.lv_fam_members)
    private SDProgressPullToRefreshListView lv_fam_members;

    private LiveSociatyMembersAdapter adapter;
    private List<UserModel> listModel;

    private PageModel pageModel = new PageModel();
    private int page = 1;

    private SDTabUnderline tab_live_menb, tab_live_apply, tab_live_logout;

    private SDDialogConfirm mDialog;
    private int rs_count;//成员人数
    private int user_id;
    private UserModel itemModel;
    private int state = 1;//0申请加入待审核、1加入申请通过、2 加入申请被拒绝，3申请退出公会待审核 4退出公会申请通过 5.退出公会申请被拒

    @Override
    protected int onCreateContentView()
    {
        return R.layout.frag_live_family_members;
    }

    @Override
    protected void init()
    {
        super.init();
        initData();
    }

    private void initData()
    {
        listModel = new ArrayList<>();
        adapter = new LiveSociatyMembersAdapter(listModel, getActivity());
        lv_fam_members.setAdapter(adapter);
        initPullToRefresh();

        /**
         * 用户详情
         */
        adapter.setItemClickCallback(new SDItemClickCallback<UserModel>()
        {
            @Override
            public void onItemClick(int position, UserModel item, View view)
            {
                Intent intent = new Intent(getActivity(), LiveUserHomeActivity.class);
                intent.putExtra(LiveUserHomeActivity.EXTRA_USER_ID, item.getUser_id());
                intent.putExtra(LiveUserHomeActivity.EXTRA_FAMILY, LiveUserHomeActivity.EXTRA_FAMILY);
                startActivity(intent);
            }
        });

        /**
         * 踢出家族
         */
        adapter.setClickDelListener(new SDItemClickCallback<UserModel>()
        {
            @Override
            public void onItemClick(int position, UserModel item, View view)
            {
                user_id = Integer.parseInt(item.getUser_id());
                itemModel = item;
                showDelDialog("是否踢出该公会成员？", "确定", "取消");
            }
        });
    }

    /**
     * 踢出公会成员对话框
     *
     * @param content     内容提示
     * @param confirmText 确认
     * @param cancelText  取消
     */
    private void showDelDialog(String content, String confirmText, String cancelText)
    {
        if (mDialog == null)
        {
            mDialog = new SDDialogConfirm(getActivity());
            mDialog.setTextGravity(Gravity.CENTER);
            mDialog.setCancelable(false);
            mDialog.setmListener(new SDDialogCustom.SDDialogCustomListener()
            {
                @Override
                public void onClickCancel(View v, SDDialogCustom dialog)
                {

                }

                @Override
                public void onClickConfirm(View v, SDDialogCustom dialog)
                {
                    delSociatyMember(user_id, itemModel);
                }

                @Override
                public void onDismiss(SDDialogCustom dialog)
                {

                }
            });
        }
        mDialog.setTextContent(content);
        mDialog.setTextConfirm(confirmText);
        mDialog.setTextCancel(cancelText);
        mDialog.show();
    }

    private void initPullToRefresh()
    {
        lv_fam_members.setMode(PullToRefreshBase.Mode.BOTH);
        lv_fam_members.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>()
        {

            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView)
            {
                refreshViewer();
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView)
            {
                loadMoreViewer();
            }
        });
    }

    public void refreshViewer()
    {
        page = 1;
        requestFamilyMembersList(false);
    }

    private void loadMoreViewer()
    {
        if (pageModel.getHas_next() == 1)
        {
            page++;
            requestFamilyMembersList(true);
        } else
        {
            SDToast.showToast("没有更多数据了");
            lv_fam_members.onRefreshComplete();
        }
    }

    /**
     * 获取公会成员列表
     *
     * @param isLoadMore
     */
    private void requestFamilyMembersList(final boolean isLoadMore)
    {
        UserModel dao = UserModelDao.query();
        CommonInterface.requestSociatyMembersList(dao.getSociety_id(), state, page, new AppRequestCallback<App_sociaty_user_listActModel>()
        {
            @Override
            protected void onSuccess(SDResponse resp)
            {
                if (actModel.getStatus() == 1)
                {
                    rs_count = actModel.getRs_count();
                    tab_live_menb.setTextTitle("公会成员(" + rs_count + ")");
                    tab_live_apply.setTextTitle("成员申请(" + actModel.getApply_count() + ")");
                    tab_live_logout.setTextTitle("退出申请(" + actModel.getQuit_count() + ")");
                    pageModel = actModel.getPage();
                    SDViewUtil.updateAdapterByList(listModel, actModel.getList(), adapter, isLoadMore);
                }
            }

            @Override
            protected void onFinish(SDResponse resp)
            {
                super.onFinish(resp);
                lv_fam_members.onRefreshComplete();
            }
        });
    }

    public void setMembRsCount(SDTabUnderline textView, SDTabUnderline textView2, SDTabUnderline textView3)
    {
        this.tab_live_menb = textView;
        this.tab_live_apply = textView2;
        this.tab_live_logout = textView3;
    }

    /**
     * 公会成员移除
     *
     * @param user_id
     */
    private void delSociatyMember(int user_id, final UserModel item)
    {
        CommonInterface.requestDelSociatyMember(user_id, new AppRequestCallback<App_sociaty_joinActModel>()
        {
            @Override
            protected void onSuccess(SDResponse sdResponse)
            {
                if (actModel.getStatus() == 1)
                {
                    SDToast.showToast("该公会成员已踢出公会");
                    adapter.removeData(item);
                    if (rs_count > 1)
                    {
                        rs_count = rs_count - 1;
                        tab_live_menb.setTextTitle("公会成员(" + rs_count + ")");
                    }
                }
            }

            @Override
            protected void onError(SDResponse resp)
            {
                super.onError(resp);
            }
        });
    }
}
