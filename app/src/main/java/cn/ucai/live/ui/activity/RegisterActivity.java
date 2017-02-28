package cn.ucai.live.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.ucai.live.I;
import cn.ucai.live.LiveHelper;
import cn.ucai.live.R;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.data.NetDao;
import cn.ucai.live.data.model.Result;
import cn.ucai.live.utils.CommonUtils;
import cn.ucai.live.utils.MD5;
import cn.ucai.live.utils.OnCompleteListener;
import cn.ucai.live.utils.ResultUtils;

public class RegisterActivity extends BaseActivity {

    @BindView(R.id.email)
    EditText username;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.nick)
    EditText nickname;
    @BindView(R.id.password_confirm)
    EditText confirmPassword;
    @BindView(R.id.register)
    Button register;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    String name,nick,pwd,confirm_pwd;
    ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = username.getText().toString().trim();
                pwd = password.getText().toString().trim();
                confirm_pwd = confirmPassword.getText().toString().trim();
                nick = nickname.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                  CommonUtils.showShortToast(R.string.User_name_cannot_be_empty);
                    username.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(nick)) {
                    CommonUtils.showShortToast(R.string.toast_nick_not_isnull);
                    nickname.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(pwd)) {
                    CommonUtils.showShortToast(R.string.Password_cannot_be_empty);
                    password.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(confirm_pwd)) {
                    CommonUtils.showShortToast(R.string.Confirm_password_cannot_be_empty);
                    confirmPassword.requestFocus();
                    return;
                } else if (!pwd.equals(confirm_pwd)) {
                    CommonUtils.showShortToast(R.string.Two_input_password);
                    return;
                }
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pwd)) {
                    pd = new ProgressDialog(RegisterActivity.this);
                    pd.setMessage(getResources().getString(R.string.Is_the_registered));
                    pd.show();
                    registerAppService();
                }
            }
        });
    }

    private void registerAppService() {
        NetDao.register(this, name, nick, pwd, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String str) {
                if (str != null) {
                    Result result = ResultUtils.getResultFromJson(str, null);
                    if(result!=null){
                        if(result.isRetMsg()){
                            registerEMServer();
                        }else {
                            pd.dismiss();
                            if(result.getRetCode()== I.MSG_REGISTER_USERNAME_EXISTS){
                                CommonUtils.showShortToast(R.string.User_already_exists);
                            }else {
                                pd.dismiss();
                                CommonUtils.showShortToast(R.string.Registration_failed);
                            }
                        }

                    }else {
                        pd.dismiss();
                        CommonUtils.showShortToast(R.string.Registration_failed);
                    }
                } else {
                    pd.dismiss();
                    CommonUtils.showShortToast(R.string.Registration_failed);
                }
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                CommonUtils.showShortToast(error);
            }
        });
    }
    private void registerEMServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // call method in SDK
                    EMClient.getInstance().createAccount(name, MD5.getMessageDigest(pwd));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            // save current user
                            LiveHelper.getInstance().setCurrentUserName(name);
                            showToast("注册成功");
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                } catch (final HyphenateException e) {
                    unRegisterAppServer();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            int errorCode = e.getErrorCode();
                            if (errorCode == EMError.NETWORK_ERROR) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void unRegisterAppServer() {
        NetDao.unRegister(this, name, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                Log.e("main","unRegisterApp(),result:"+result);
            }

            @Override
            public void onError(String error) {

            }
        });
    }
}
