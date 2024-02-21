package uz.unnarsx.cherrygram.preferences;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.ThemePreviewMessagesCell;
import org.telegram.ui.Components.AnimatedColor;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FilledTabsView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumGradient;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.Stories.StoriesUtilities;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.ArrayList;

import uz.unnarsx.cherrygram.CGFeatureHooks;
import uz.unnarsx.cherrygram.CherrygramConfig;
import uz.unnarsx.cherrygram.extras.CherrygramExtras;
import uz.unnarsx.cherrygram.helpers.AppRestartHelper;
import uz.unnarsx.cherrygram.helpers.ChatsHelper;
import uz.unnarsx.cherrygram.helpers.ui.PopupHelper;

public class MessagesAndProfilesPreferencesEntry extends BaseFragment {

    private FrameLayout contentView;
    private ColoredActionBar colorBar;

    private final int PAGE_MESSAGE = 0;
    private final int PAGE_PROFILE = 1;

    public Page messagePage;
    public Page profilePage;

    public Page getCurrentPage() {
        return viewPager.getCurrentPosition() == 0 ? messagePage : profilePage;
    }

    public final static long CHERRY_EMOJI_ID = 5203934494985307875L;
    public final static int PROFILE_BACKGROUND_COLOR_ID = 14;
    public final static int REPLY_BACKGROUND_COLOR_ID = 13;

    private class Page extends FrameLayout {

        private ProfilePreview profilePreview;

        private RecyclerListView listView;
        private RecyclerView.Adapter listAdapter;

        private int selectedColor;
        private long selectedEmoji;
        private ThemePreviewMessagesCell messagesCellPreview;

        int rowCount;

        int previewRow = -1;
        int messagePreviewDivisorRow = -1;
        int headerRow = -1;

        int timeWithSecondsRow = -1;
        int premiumStatusRow = -1;
        int replyBackgroundRow = -1;
        int replyColorRow = -1;
        int replyEmojiRow = -1;

        int infoHeaderRow;
        int idRow;
        int idDcRow;

        int showDcIdRow = -1;
        int profilePreviewDivisorRow = -1;
        int profileBackgroundRow = -1;
        int profileEmojiRow = -1;

        private final int VIEW_TYPE_MESSAGE = 0;
        private final int VIEW_TYPE_HEADER = 1;
        private final int VIEW_TYPE_SWITCH = 2;
        private final int VIEW_TYPE_TEXT_SETTING = 3;
        private final int VIEW_TYPE_TEXT_DETAIL = 4;
        private final int VIEW_TYPE_SHADOW = 5;

        private final int type;
        public Page(Context context, int type) {
            super(context);
            this.type = type;

            TLRPC.User user = getUserConfig().getCurrentUser();

            if (type == PAGE_PROFILE) {
                if (user.premium && UserObject.getProfileColorId(user) != -1) {
                    selectedColor = CherrygramConfig.INSTANCE.getProfileBackgroundColor() ? UserObject.getProfileColorId(user) : -1;
                } else {
                    selectedColor = CherrygramConfig.INSTANCE.getProfileBackgroundColor() ? PROFILE_BACKGROUND_COLOR_ID : -1;
                }

                if (user.premium && UserObject.getProfileEmojiId(user) != 0) {
                    selectedEmoji = CherrygramConfig.INSTANCE.getProfileBackgroundEmoji() ? UserObject.getProfileEmojiId(user) : 0;
                } else {
                    selectedEmoji = CherrygramConfig.INSTANCE.getProfileBackgroundEmoji() ? CHERRY_EMOJI_ID : 0;
                }
            } else {
                if (user.premium && UserObject.getColorId(user) != -1) {
                    selectedColor = CherrygramConfig.INSTANCE.getReplyCustomColors() ? UserObject.getColorId(user) : -1;
                } else {
                    selectedColor = CherrygramConfig.INSTANCE.getReplyCustomColors() ? REPLY_BACKGROUND_COLOR_ID : -1;
                }

                if (user.premium && UserObject.getEmojiId(user) != 0) {
                    selectedEmoji = CherrygramConfig.INSTANCE.getReplyBackgroundEmoji() ? UserObject.getEmojiId(user) : 0;
                } else {
                    selectedEmoji = CherrygramConfig.INSTANCE.getReplyBackgroundEmoji() ? CHERRY_EMOJI_ID : 0;
                }
            }

            listView = new RecyclerListView(getContext(), getResourceProvider()) {
                @Override
                protected void onMeasure(int widthSpec, int heightSpec) {
                    super.onMeasure(widthSpec, heightSpec);
                }

                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    super.onLayout(changed, l, t, r, b);
                }
            };
            ((DefaultItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);
            listView.setLayoutManager(new LinearLayoutManager(getContext()));
            listView.setAdapter(listAdapter = new RecyclerListView.SelectionAdapter() {
                @Override
                public boolean isEnabled(RecyclerView.ViewHolder holder) {
                    return holder.getItemViewType() == VIEW_TYPE_SWITCH;
                }

                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view;
                    switch (viewType) {
                        case VIEW_TYPE_MESSAGE:
                            ThemePreviewMessagesCell messagesCell = messagesCellPreview = new ThemePreviewMessagesCell(
                                    getContext(), parentLayout, user.premium
                                    && UserObject.getEmojiId(UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser()) != 0
                                            ? ThemePreviewMessagesCell.TYPE_PEER_COLOR : ThemePreviewMessagesCell.TYPE_PEER_COLOR_CHERRY
                            );
                            messagesCell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                            messagesCell.fragment = MessagesAndProfilesPreferencesEntry.this;
                            view = messagesCell;
                            break;
                        case VIEW_TYPE_HEADER:
                            HeaderCell header = new HeaderCell(getContext());
                            header.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                            view = header;
                            break;
                        case VIEW_TYPE_SWITCH:
                            TextCheckCell switchCell = new TextCheckCell(getContext());
                            switchCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                            view = switchCell;
                            break;
                        case VIEW_TYPE_TEXT_SETTING:
                            TextSettingsCell textSettingsCell = new TextSettingsCell(getContext());
                            textSettingsCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                            view = textSettingsCell;
                            break;
                        case VIEW_TYPE_TEXT_DETAIL:
                            TextDetailCell textDetailCell = new TextDetailCell(getContext(), getResourceProvider(), true);
                            textDetailCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                            textDetailCell.setContentDescriptionValueFirst(true);
                            view = textDetailCell;
                            break;
                        case VIEW_TYPE_SHADOW:
                        default: {
                            view = new ShadowSectionCell(getContext());
                            break;
                        }
                    }
                    return new RecyclerListView.Holder(view);
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    switch (getItemViewType(position)) {
                        case VIEW_TYPE_HEADER: {
                            HeaderCell headerCell = (HeaderCell) holder.itemView;
                            if (position == infoHeaderRow) {
                                headerCell.setText(LocaleController.getString("Info", R.string.Info));
                            } else if (position == headerRow) {
                                if (type == PAGE_MESSAGE) {
                                    headerCell.setText(LocaleController.getString("CP_CustomizeReplyBackground", R.string.CP_CustomizeMessage));
                                } else {
                                    headerCell.setText(LocaleController.getString("CP_CustomizeProfileBackground", R.string.CP_CustomizeProfile));
                                }
                            }
                            break;
                        }
                        case VIEW_TYPE_SWITCH: {
                            TextCheckCell switchCell = (TextCheckCell) holder.itemView;
                            switchCell.updateRTL();
                            if (position == timeWithSecondsRow) {
                                switchCell.setTextAndValueAndCheck(LocaleController.getString("CP_ShowSeconds", R.string.CP_ShowSeconds), LocaleController.getString("CP_ShowSeconds_Desc", R.string.CP_ShowSeconds_Desc), CherrygramConfig.INSTANCE.getShowSeconds(), true, true);
                            } else if (position == premiumStatusRow) {
                                switchCell.setTextAndValueAndCheck(LocaleController.getString("CP_DisablePremiumStatuses", R.string.CP_DisablePremiumStatuses), LocaleController.getString("CP_DisablePremiumStatuses_Desc", R.string.CP_DisablePremiumStatuses_Desc), CherrygramConfig.INSTANCE.getDisablePremiumStatuses(), true, true);
                            } else if (position == replyBackgroundRow) {
                                switchCell.setTextAndCheck(LocaleController.getString("CP_ReplyBackground", R.string.CP_ReplyBackground), CherrygramConfig.INSTANCE.getReplyBackground(), false);
                            } else if (position == replyColorRow) {
                                switchCell.setTextAndCheck(LocaleController.getString("CP_ReplyCustomColors", R.string.CP_ReplyCustomColors), CherrygramConfig.INSTANCE.getReplyCustomColors(), false);
                            } else if (position == replyEmojiRow) {
                                switchCell.setTextAndCheck(LocaleController.getString("CP_ReplyBackgroundEmoji", R.string.CP_ReplyBackgroundEmoji), CherrygramConfig.INSTANCE.getReplyBackgroundEmoji(), false);
                            } else if (position == profileBackgroundRow) {
                                switchCell.setTextAndCheck(LocaleController.getString("CP_ProfileBackgroundColor", R.string.CP_ProfileBackgroundColor), CherrygramConfig.INSTANCE.getProfileBackgroundColor(), false);
                            } else if (position == profileEmojiRow) {
                                switchCell.setTextAndCheck(LocaleController.getString("CP_ProfileBackgroundEmoji", R.string.CP_ProfileBackgroundEmoji), CherrygramConfig.INSTANCE.getProfileBackgroundEmoji(), true);
                            }
                            break;
                        }
                        case VIEW_TYPE_TEXT_SETTING: {
                            TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                            textSettingsCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                            if (position == showDcIdRow) {
                                textSettingsCell.setTextAndValue(LocaleController.getString("AP_ShowID", R.string.AP_ShowID), CGFeatureHooks.getShowDcIdText(), true);
                            }
                            break;
                        }
                        case VIEW_TYPE_TEXT_DETAIL: {
                            TextDetailCell detailCell = (TextDetailCell) holder.itemView;
                            final TLRPC.User me = getUserConfig().getCurrentUser();

                            if (position == idRow) {
                                detailCell.setTextAndValue(me.id + "", "ID", false);
                            } else if (position == idDcRow) {
                                StringBuilder sb = new StringBuilder();
                                if (me.photo != null && me.photo.dc_id > 0) {
                                    sb = new StringBuilder();
                                    sb.append("DC: ");
                                    sb.append(me.photo.dc_id);
                                    sb.append(", ");
                                    sb.append(CherrygramExtras.INSTANCE.getDCName(me.photo.dc_id));
                                    sb.append(", ");
                                    sb.append(CherrygramExtras.INSTANCE.getDCGeo(me.photo.dc_id));
                                } else {
                                    sb.append("DC: ");
                                    sb.append(LocaleController.getString("NumberUnknown", R.string.NumberUnknown));
                                }
                                detailCell.setTextAndValue("ID: " + me.id, sb, false);
                            }
                            break;
                        }
                    }
                }

                @Override
                public int getItemCount() {
                    return rowCount;
                }

                @Override
                public int getItemViewType(int position) {
                    if (position == previewRow) {
                        return VIEW_TYPE_MESSAGE;
                    }
                    if (position == infoHeaderRow || position == headerRow) {
                        return VIEW_TYPE_HEADER;
                    }
                    if (position == timeWithSecondsRow || position == premiumStatusRow || position == replyBackgroundRow || position == replyColorRow || position == replyEmojiRow || position == profileBackgroundRow || position == profileEmojiRow) {
                        return VIEW_TYPE_SWITCH;
                    }
                    if (position == showDcIdRow) {
                        return VIEW_TYPE_TEXT_SETTING;
                    }
                    if (position == idRow || position == idDcRow) {
                        return VIEW_TYPE_TEXT_DETAIL;
                    }
                    return VIEW_TYPE_SHADOW;
                }
            });
            listView.setOnItemClickListener((view, position) -> {
                final TLRPC.User me = getUserConfig().getCurrentUser();

                if (position == timeWithSecondsRow) {
                    CherrygramConfig.INSTANCE.toggleShowSeconds();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getShowSeconds());
                    }
                    LocaleController.getInstance().recreateFormatters();
                    parentLayout.rebuildAllFragmentViews(true, true);
                } else if (position == premiumStatusRow) {
                    CherrygramConfig.INSTANCE.toggleDisablePremiumStatuses();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getDisablePremiumStatuses());
                    }

                    if (!CherrygramConfig.INSTANCE.getDisablePremiumStatuses()) {
                        profilePage.profilePreview.titleView.setRightDrawable(profilePage.profilePreview.statusEmoji);
                        profilePage.profilePreview.statusEmoji.play();
                    } else {
                        profilePage.profilePreview.titleView.setRightDrawable(0);
                    }

                    updateMessages();
                    if (type == PAGE_PROFILE) {
                        messagePage.updateMessages();
                        messagePage.updateRows();
                    } else {
                        profilePage.updateRows();
                    }
                } else if (position == replyBackgroundRow) {
                    CherrygramConfig.INSTANCE.toggleReplyBackground();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getReplyBackground());
                    }

                    updateMessages();
                } else if (position == replyColorRow) {
                    CherrygramConfig.INSTANCE.toggleReplyCustomColors();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getReplyCustomColors());
                    }

                    if (me.premium && UserObject.getColorId(me) != -1) {
                        selectedColor = CherrygramConfig.INSTANCE.getReplyCustomColors() ? UserObject.getColorId(me) : -1;
                    } else {
                        selectedColor = CherrygramConfig.INSTANCE.getReplyCustomColors() ? REPLY_BACKGROUND_COLOR_ID : -1;
                    }

                    updateMessages();
                } else if (position == replyEmojiRow) {
                    CherrygramConfig.INSTANCE.toggleReplyBackgroundEmoji();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getReplyBackgroundEmoji());
                    }

                    if (me.premium && UserObject.getEmojiId(me) != 0) {
                        selectedEmoji = CherrygramConfig.INSTANCE.getReplyBackgroundEmoji() ? UserObject.getEmojiId(me) : 0;
                    } else {
                        selectedEmoji = CherrygramConfig.INSTANCE.getReplyBackgroundEmoji() ? CHERRY_EMOJI_ID : 0;
                    }

                    updateMessages();
                } else if (position == showDcIdRow) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    ArrayList<Integer> types = new ArrayList<>();
                    arrayList.add(LocaleController.getString("Disable", R.string.Disable));
                    types.add(CherrygramConfig.ID_DC_NONE);
                    arrayList.add("ID");
                    types.add(CherrygramConfig.ID_ONLY);
                    arrayList.add("ID + DC");
                    types.add(CherrygramConfig.ID_DC);
                    PopupHelper.show(arrayList, LocaleController.getString("AP_ShowID", R.string.AP_ShowID), types.indexOf(CherrygramConfig.INSTANCE.getShowIDDC()), context, i -> {
                        CherrygramConfig.INSTANCE.setShowIDDC(types.get(i));
                        listAdapter.notifyItemChanged(showDcIdRow);
                        listAdapter.notifyItemChanged(idRow);
                        listAdapter.notifyItemChanged(idDcRow);
                        profilePage.updateRows();
                        parentLayout.rebuildAllFragmentViews(false, false);
                    });
                } else if (position == profileBackgroundRow) {
                    CherrygramConfig.INSTANCE.toggleProfileBackgroundColor();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getProfileBackgroundColor());
                    }

                    if (me.premium && UserObject.getProfileColorId(me) != -1) {
                        selectedColor = CherrygramConfig.INSTANCE.getProfileBackgroundColor() ? UserObject.getProfileColorId(me) : -1;
                    } else {
                        selectedColor = CherrygramConfig.INSTANCE.getProfileBackgroundColor() ? PROFILE_BACKGROUND_COLOR_ID : -1;
                    }

                    updateMessages();
                    if (type == PAGE_PROFILE) {
                        messagePage.updateMessages();
                    }
                    if (type == PAGE_PROFILE && colorBar != null) {
                        colorBar.setColor(currentAccount, selectedColor, true);
                    }
                    if (profilePreview != null) {
                        profilePreview.setColor(selectedColor, true);
                    }
                    if (profilePage != null && profilePage.profilePreview != null && messagePage != null) {
                        profilePage.profilePreview.overrideAvatarColor(messagePage.selectedColor);
                    }
                } else if (position == profileEmojiRow) {
                    CherrygramConfig.INSTANCE.toggleProfileBackgroundEmoji();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(CherrygramConfig.INSTANCE.getProfileBackgroundEmoji());
                    }

                    if (me.premium && UserObject.getProfileEmojiId(me) != 0) {
                        selectedEmoji = CherrygramConfig.INSTANCE.getProfileBackgroundEmoji() ? UserObject.getProfileEmojiId(me) : 0;
                    } else {
                        selectedEmoji = CherrygramConfig.INSTANCE.getProfileBackgroundEmoji() ? CHERRY_EMOJI_ID : 0;
                    }

                    updateMessages();
                    if (type == PAGE_PROFILE) {
                        messagePage.updateMessages();
                    }
                    if (profilePreview != null) {
                        profilePreview.setEmoji(selectedEmoji, true);
                    }
                }
            });
            addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            FrameLayout buttonContainer = new FrameLayout(getContext());
            buttonContainer.setPadding(dp(14), dp(14), dp(14), dp(14));
            buttonContainer.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

            String buttonText = LocaleController.getString(type == PAGE_PROFILE ? R.string.EditProfileColor : R.string.ChangeUserNameColor);
            int buttonImage = type == PAGE_PROFILE ? R.drawable.msg_colors : R.drawable.msg_palette;

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append("..").setSpan(new ColoredImageSpan(ContextCompat.getDrawable(context, buttonImage)), 0, 1, 0);
            sb.setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(8)), 1, 2, 0);
            sb.append(buttonText);
            sb.append(".").setSpan(new DialogCell.FixedWidthSpan(AndroidUtilities.dp(5)), sb.length() - 1, sb.length(), 0);

            ButtonWithCounterView button = new ButtonWithCounterView(getContext(), getResourceProvider());
            button.text.setHacks(true, true, true);
            button.setText(sb, false);
            button.setOnClickListener(v -> buttonClick());
            buttonContainer.addView(button, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
            if (getUserConfig().isPremium()) addView(buttonContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

            DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
            itemAnimator.setDurations(350);
            itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            itemAnimator.setDelayAnimations(false);
            itemAnimator.setSupportsChangeAnimations(false);
            listView.setItemAnimator(itemAnimator);

            if (type == PAGE_PROFILE) {
                profilePreview = new ProfilePreview(getContext(), currentAccount, resourceProvider);
                profilePreview.setColor(selectedColor, false);
                profilePreview.setEmoji(selectedEmoji, false);
                addView(profilePreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL));
            }

            updateColors();
            updateRows();

            setWillNotDraw(false);
        }

        private void buttonClick() {
            if (type == PAGE_PROFILE) {
                presentFragment(new PeerColorActivity(0).startOnProfile());
            } else {
                presentFragment(new PeerColorActivity(0));
            }
        }

        private int actionBarHeight;

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (getParentLayout() != null) {
                getParentLayout().drawHeaderShadow(canvas, actionBarHeight);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (type == PAGE_MESSAGE) {
                actionBarHeight = ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight;
                ((MarginLayoutParams) listView.getLayoutParams()).topMargin = actionBarHeight;
            } else {
                actionBarHeight = dp(144) + AndroidUtilities.statusBarHeight;
                ((MarginLayoutParams) listView.getLayoutParams()).topMargin = actionBarHeight;
                ((MarginLayoutParams) profilePreview.getLayoutParams()).height = actionBarHeight;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        private void updateRows() {
            rowCount = 0;
            if (type == PAGE_MESSAGE) {
                previewRow = rowCount++;
                messagePreviewDivisorRow = rowCount++;
                headerRow = rowCount++;
                timeWithSecondsRow = rowCount++;

                premiumStatusRow = rowCount++;
                if (listAdapter != null) {
                    listAdapter.notifyItemRemoved(premiumStatusRow);
                    listAdapter.notifyItemInserted(premiumStatusRow);
                }
                replyBackgroundRow = rowCount++;
                replyColorRow = rowCount++;
                replyEmojiRow = rowCount++;
            }
            if (type == PAGE_PROFILE) {
                // DC ID
                int prevInfoHeaderRow = infoHeaderRow;
                infoHeaderRow = -1;
                if (CherrygramConfig.INSTANCE.getShowIDDC() == CherrygramConfig.ID_ONLY || CherrygramConfig.INSTANCE.getShowIDDC() == CherrygramConfig.ID_DC) {
                    infoHeaderRow = rowCount++;
                }
                if (listAdapter != null) {
                    if (prevInfoHeaderRow == -1 && infoHeaderRow != -1) {
                        listAdapter.notifyItemInserted(infoHeaderRow);
                    } else if (prevInfoHeaderRow != -1 && infoHeaderRow == -1) {
                        listAdapter.notifyItemRemoved(prevInfoHeaderRow);
                    }
                }

                int prevIdRow = idRow;
                idRow = -1;
                if (CherrygramConfig.INSTANCE.getShowIDDC() == CherrygramConfig.ID_ONLY) {
                    idRow = rowCount++;
                }
                if (listAdapter != null) {
                    if (prevIdRow == -1 && idRow != -1) {
                        listAdapter.notifyItemInserted(idRow);
                    } else if (prevIdRow != -1 && idRow == -1) {
                        listAdapter.notifyItemRemoved(prevIdRow);
                    }
                }

                int prevIdDcRow = idDcRow;
                idDcRow = -1;
                if (CherrygramConfig.INSTANCE.getShowIDDC() == CherrygramConfig.ID_DC) {
                    idDcRow = rowCount++;
                }
                if (listAdapter != null) {
                    if (prevIdDcRow == -1 && idDcRow != -1) {
                        listAdapter.notifyItemInserted(idDcRow);
                    } else if (prevIdDcRow != -1 && idDcRow == -1) {
                        listAdapter.notifyItemRemoved(prevIdDcRow);
                    }
                }
                // DC ID

                profilePreviewDivisorRow = rowCount++;
                headerRow = rowCount++;
                showDcIdRow = rowCount++;
                premiumStatusRow = rowCount++;
                if (listAdapter != null) {
                    listAdapter.notifyItemRemoved(premiumStatusRow);
                    listAdapter.notifyItemInserted(premiumStatusRow);
                }
                profileBackgroundRow = rowCount++;
                profileEmojiRow = rowCount++;
            }
        }

        private void updateMessages() {
            if (messagesCellPreview != null) {
                ChatMessageCell[] cells = messagesCellPreview.getCells();
                for (int i = 0; i < cells.length; ++i) {
                    if (cells[i] != null) {
                        MessageObject msg = cells[i].getMessageObject();
                        if (msg != null) {
                            msg.overrideLinkColor = selectedColor;
                            msg.overrideLinkEmoji = selectedEmoji;
                            cells[i].setAvatar(msg);
                            if (!CherrygramConfig.INSTANCE.getDisablePremiumStatuses()) {
                                Long emojiStatusId = UserObject.getEmojiStatusDocumentId(getUserConfig().getCurrentUser());
                                if (emojiStatusId != null) {
                                    cells[i].currentNameStatusDrawable.set(emojiStatusId, true);
                                    cells[i].currentNameStatusDrawable.play();
                                } else {
                                    cells[i].currentNameStatusDrawable.set(PremiumGradient.getInstance().premiumStarDrawableMini, true);
                                }
                            } else {
                                cells[i].currentNameStatusDrawable.set(0, false);
                            }
                            cells[i].invalidate();
                        }
                    }
                }
            }
        }

        public void updateColors() {
            listView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
            if (type == PAGE_PROFILE && colorBar != null) {
                colorBar.setColor(currentAccount, selectedColor, true);
            }
            if (messagesCellPreview != null) {
                messagesCellPreview.invalidate();
            }
            if (profilePreview != null) {
                profilePreview.setColor(selectedColor, false);
            }
        }

    }

    private Theme.ResourcesProvider parentResourcesProvider;
    private final SparseIntArray currentColors = new SparseIntArray();
    private final Theme.MessageDrawable msgInDrawable, msgInDrawableSelected;

    public MessagesAndProfilesPreferencesEntry() {
        super();

        resourceProvider = new Theme.ResourcesProvider() {
            @Override
            public int getColor(int key) {
                int index = currentColors.indexOfKey(key);
                if (index >= 0) {
                    return currentColors.valueAt(index);
                }
                if (parentResourcesProvider != null) {
                    return parentResourcesProvider.getColor(key);
                }
                return Theme.getColor(key);
            }

            @Override
            public Drawable getDrawable(String drawableKey) {
                if (drawableKey.equals(Theme.key_drawable_msgIn)) {
                    return msgInDrawable;
                }
                if (drawableKey.equals(Theme.key_drawable_msgInSelected)) {
                    return msgInDrawableSelected;
                }
                if (parentResourcesProvider != null) {
                    return parentResourcesProvider.getDrawable(drawableKey);
                }
                return Theme.getThemeDrawable(drawableKey);
            }

            @Override
            public Paint getPaint(String paintKey) {
                return Theme.ResourcesProvider.super.getPaint(paintKey);
            }
        };
        msgInDrawable = new Theme.MessageDrawable(Theme.MessageDrawable.TYPE_TEXT, false, false, resourceProvider);
        msgInDrawableSelected = new Theme.MessageDrawable(Theme.MessageDrawable.TYPE_TEXT, false, true, resourceProvider);
    }

    @Override
    public void setResourceProvider(Theme.ResourcesProvider resourceProvider) {
        parentResourcesProvider = resourceProvider;
    }

    private boolean startAtProfile;
    public MessagesAndProfilesPreferencesEntry startOnProfile() {
        this.startAtProfile = true;
        return this;
    }

    @Override
    public boolean onFragmentCreate() {
        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public int getBottomOffset(int tag) {
                return dp(62);
            }

            @Override
            public boolean clipWithGradient(int tag) {
                return true;
            }
        });
        getMediaDataController().loadReplyIcons();
        if (MessagesController.getInstance(currentAccount).peerColors == null && BuildVars.DEBUG_PRIVATE_VERSION) {
            MessagesController.getInstance(currentAccount).loadAppConfig(true);
        }
        return super.onFragmentCreate();
    }

    private ViewPagerFixed viewPager;
    private ImageView backButton;
    private FrameLayout actionBarContainer;
    private FilledTabsView tabsView;

    @Override
    public View createView(Context context) {
        messagePage = new Page(context, PAGE_MESSAGE);
        profilePage = new Page(context, PAGE_PROFILE);

        actionBar.setCastShadows(false);
        actionBar.setVisibility(View.GONE);
        actionBar.setAllowOverlayTitle(false);

        FrameLayout frameLayout = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                if (actionBarContainer != null) {
                    ((MarginLayoutParams) actionBarContainer.getLayoutParams()).height = ActionBar.getCurrentActionBarHeight();
                    ((MarginLayoutParams) actionBarContainer.getLayoutParams()).topMargin = AndroidUtilities.statusBarHeight;
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        frameLayout.setFitsSystemWindows(true);

        colorBar = new ColoredActionBar(context, resourceProvider) {
            @Override
            protected void onUpdateColor() {
                updateLightStatusBar();
                updateActionBarButtonsColor();
                if (tabsView != null) {
                    tabsView.setBackgroundColor(getTabsViewBackgroundColor());
                }
            }

            private int lastBtnColor = 0;
            public void updateActionBarButtonsColor() {
                final int btnColor = getActionBarButtonColor();
                if (lastBtnColor != btnColor) {
                    if (backButton != null) {
                        lastBtnColor = btnColor;
                        backButton.setColorFilter(new PorterDuffColorFilter(btnColor, PorterDuff.Mode.SRC_IN));
                    }
                }
            }
        };
        if (profilePage != null) {
            colorBar.setColor(currentAccount, profilePage.selectedColor, false);
        }
        frameLayout.addView(colorBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        viewPager = new ViewPagerFixed(context) {
            @Override
            protected void onTabAnimationUpdate(boolean manual) {
                tabsView.setSelected(viewPager.getPositionAnimated());
                colorBar.setProgressToGradient(viewPager.getPositionAnimated());
            }
        };
        viewPager.setAdapter(new ViewPagerFixed.Adapter() {
            @Override
            public int getItemCount() {
                return 2;
            }

            @Override
            public View createView(int viewType) {
                if (viewType == PAGE_MESSAGE) return messagePage;
                if (viewType == PAGE_PROFILE) return profilePage;
                return null;
            }

            @Override
            public int getItemViewType(int position) {
                return position;
            }

            @Override
            public void bindView(View view, int position, int viewType) {

            }
        });
        frameLayout.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        actionBarContainer = new FrameLayout(context);
        frameLayout.addView(actionBarContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        tabsView = new FilledTabsView(context);
        tabsView.setTabs(
                LocaleController.getString(R.string.Message),
                LocaleController.getString(R.string.UserColorTabProfile)
        );
        tabsView.onTabSelected(tab -> {
            if (viewPager != null) {
                viewPager.scrollToPosition(tab);
            }
        });
        actionBarContainer.addView(tabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 40, Gravity.CENTER));

        if (startAtProfile) {
            viewPager.setPosition(1);
            if (tabsView != null) {
                tabsView.setSelected(1);
            }
            if (colorBar != null) {
                colorBar.setProgressToGradient(1f);
                updateLightStatusBar();
            }
        }

        backButton = new ImageView(context);
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_actionBarWhiteSelector), Theme.RIPPLE_MASK_CIRCLE_20DP));
        backButton.setImageResource(R.drawable.ic_ab_back);
        backButton.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        backButton.setOnClickListener(v -> {
            if (onBackPressed()) {
                finishFragment();
            }
        });
        actionBarContainer.addView(backButton, LayoutHelper.createFrame(54, 54, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        colorBar.updateColors();

        fragmentView = contentView = frameLayout;
        return contentView;
    }

    @Override
    public void onFragmentClosed() {
        super.onFragmentClosed();
        Bulletin.removeDelegate(this);
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return SimpleThemeDescription.createThemeDescriptions(this::updateColors,
                Theme.key_windowBackgroundWhite,
                Theme.key_windowBackgroundWhiteBlackText,
                Theme.key_windowBackgroundWhiteGrayText2,
                Theme.key_listSelector,
                Theme.key_windowBackgroundGray,
                Theme.key_windowBackgroundWhiteGrayText4,
                Theme.key_text_RedRegular,
                Theme.key_windowBackgroundChecked,
                Theme.key_windowBackgroundCheckText,
                Theme.key_switchTrackBlue,
                Theme.key_switchTrackBlueChecked,
                Theme.key_switchTrackBlueThumb,
                Theme.key_switchTrackBlueThumbChecked
        );
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateColors() {
        contentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        messagePage.updateColors();
        profilePage.updateColors();
        if (colorBar != null) {
            colorBar.updateColors();
        }
        setNavigationBarColor(getNavigationBarColor());
    }

    private class ColoredActionBar extends View {

        private int defaultColor;
        private final Theme.ResourcesProvider resourcesProvider;

        public ColoredActionBar(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            defaultColor = Theme.getColor(Theme.key_actionBarDefault, resourcesProvider);
            setColor(-1, -1, false);
        }

        public void setColor(int currentAccount, int colorId, boolean animated) {
            isDefault = false;
            if (colorId < 0 || currentAccount < 0) {
                isDefault = true;
                color1 = color2 = Theme.getColor(Theme.key_actionBarDefault, resourcesProvider);
            } else {
                MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                MessagesController.PeerColor peerColor = peerColors == null ? null : peerColors.getColor(colorId);
                if (peerColor != null) {
                    final boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
                    color1 = peerColor.getBgColor1(isDark);
                    color2 = peerColor.getBgColor2(isDark);
                } else {
                    isDefault = true;
                    color1 = color2 = Theme.getColor(Theme.key_actionBarDefault, resourcesProvider);
                }
            }
            if (!animated) {
                color1Animated.set(color1, true);
                color2Animated.set(color2, true);
            }
            invalidate();
        }

        private float progressToGradient = 0;
        public void setProgressToGradient(float progress) {
            if (Math.abs(progressToGradient - progress) > 0.001f) {
                progressToGradient = progress;
                onUpdateColor();
                invalidate();
            }
        }

        public boolean isDefault;
        public int color1, color2;
        private final AnimatedColor color1Animated = new AnimatedColor(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedColor color2Animated = new AnimatedColor(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);

        private int backgroundGradientColor1, backgroundGradientColor2, backgroundGradientHeight;
        private LinearGradient backgroundGradient;
        private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        protected void onUpdateColor() {

        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            final int color1 = color1Animated.set(this.color1);
            final int color2 = color2Animated.set(this.color2);
            if (backgroundGradient == null || backgroundGradientColor1 != color1 || backgroundGradientColor2 != color2 || backgroundGradientHeight != getHeight()) {
                backgroundGradient = new LinearGradient(0, 0, 0, backgroundGradientHeight = getHeight(), new int[] { backgroundGradientColor2 = color2, backgroundGradientColor1 = color1 }, new float[] { 0, 1 }, Shader.TileMode.CLAMP);
                backgroundPaint.setShader(backgroundGradient);
                onUpdateColor();
            }
            if (progressToGradient < 1) {
                canvas.drawColor(defaultColor);
            }
            if (progressToGradient > 0) {
                backgroundPaint.setAlpha((int) (0xFF * progressToGradient));
                canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
            }
        }

        protected boolean ignoreMeasure;

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, ignoreMeasure ? heightMeasureSpec : MeasureSpec.makeMeasureSpec(AndroidUtilities.statusBarHeight + dp(144), MeasureSpec.EXACTLY));
        }

        public void updateColors() {
            defaultColor = Theme.getColor(Theme.key_actionBarDefault, resourcesProvider);
            onUpdateColor();
            invalidate();
        }

        public int getColor() {
            return ColorUtils.blendARGB(Theme.getColor(Theme.key_actionBarDefault, resourcesProvider), ColorUtils.blendARGB(color1Animated.get(), color2Animated.get(), .75f), progressToGradient);
        }

        public int getActionBarButtonColor() {
            return ColorUtils.blendARGB(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), isDefault ? Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider) : Color.WHITE, progressToGradient);
        }

        public int getTabsViewBackgroundColor() {
            return (
                ColorUtils.blendARGB(
                    AndroidUtilities.computePerceivedBrightness(Theme.getColor(Theme.key_actionBarDefault, resourcesProvider)) > .721f ?
                        Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider) :
                        Theme.adaptHSV(Theme.getColor(Theme.key_actionBarDefault, resourcesProvider), +.08f, -.08f),
                    AndroidUtilities.computePerceivedBrightness(ColorUtils.blendARGB(color1Animated.get(), color2Animated.get(), .75f)) > .721f ?
                        Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon, resourcesProvider) :
                        Theme.adaptHSV(ColorUtils.blendARGB(color1Animated.get(), color2Animated.get(), .75f), +.08f, -.08f),
                    progressToGradient
                )
            );
        }
    }

    private class ProfilePreview extends FrameLayout {

        private final Theme.ResourcesProvider resourcesProvider;
        private final int currentAccount;

        private final ImageReceiver imageReceiver = new ImageReceiver(this);
        private final AvatarDrawable avatarDrawable = new AvatarDrawable();
        private final SimpleTextView titleView, subtitleView;

        private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable statusEmoji;

        private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emoji = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, false, dp(20), AnimatedEmojiDrawable.CACHE_TYPE_ALERT_PREVIEW_STATIC);

        private final StoriesUtilities.StoryGradientTools storyGradient = new StoriesUtilities.StoryGradientTools(this, false);

        public ProfilePreview(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            this.currentAccount = currentAccount;
            this.resourcesProvider = resourcesProvider;

            titleView = new SimpleTextView(context) {
                @Override
                protected void onAttachedToWindow() {
                    super.onAttachedToWindow();
                    statusEmoji.attach();
                }

                @Override
                protected void onDetachedFromWindow() {
                    super.onDetachedFromWindow();
                    statusEmoji.detach();
                }
            };
            statusEmoji = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(titleView, true, dp(24));

            Long emojiStatusId = UserObject.getEmojiStatusDocumentId(getUserConfig().getCurrentUser());
            if (emojiStatusId != null) {
                titleView.setDrawablePadding(AndroidUtilities.dp(4));
                statusEmoji.set(emojiStatusId, true);
                titleView.setRightDrawableOutside(true);
            } else {
                titleView.setDrawablePadding(AndroidUtilities.dp(6));
                statusEmoji.set(PremiumGradient.getInstance().premiumStarDrawableMini, true);
                titleView.setRightDrawableOutside(true);
            }

            if (!CherrygramConfig.INSTANCE.getDisablePremiumStatuses()) titleView.setRightDrawable(statusEmoji);
            titleView.setRightDrawableOnClick(v -> statusEmoji.play());
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setTextSize(20);
            titleView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            titleView.setScrollNonFitText(true);
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 97, 0, 16, 50.33f));

            subtitleView = new SimpleTextView(context);
            subtitleView.setTextSize(14);
            subtitleView.setTextColor(0x80FFFFFF);
            subtitleView.setScrollNonFitText(true);
            addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 97, 0, 16, 30.66f));

            imageReceiver.setRoundRadius(dp(54));
            CharSequence title;
            TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
            title = UserObject.getUserName(user);

            avatarDrawable.setInfo(currentAccount, user);
            imageReceiver.setForUserOrChat(user, avatarDrawable);
            try {
                title = Emoji.replaceEmoji(title, null, false);
            } catch (Exception ignore) {}

            titleView.setText(title);
            subtitleView.setText(LocaleController.getString(R.string.Online));

            setWillNotDraw(false);
        }

        public void overrideAvatarColor(int colorId) {
            final int color1, color2;
            if (colorId >= 14) {
                MessagesController messagesController = MessagesController.getInstance(UserConfig.selectedAccount);
                MessagesController.PeerColors peerColors = messagesController != null ? messagesController.peerColors : null;
                MessagesController.PeerColor peerColor = peerColors != null ? peerColors.getColor(colorId) : null;
                if (peerColor != null) {
                    final int peerColorValue = peerColor.getColor1();
                    color1 = getThemedColor(Theme.keys_avatar_background[AvatarDrawable.getPeerColorIndex(peerColorValue)]);
                    color2 = getThemedColor(Theme.keys_avatar_background2[AvatarDrawable.getPeerColorIndex(peerColorValue)]);
                } else {
                    color1 = getThemedColor(Theme.keys_avatar_background[AvatarDrawable.getColorIndex(colorId)]);
                    color2 = getThemedColor(Theme.keys_avatar_background2[AvatarDrawable.getColorIndex(colorId)]);
                }
            } else {
                color1 = getThemedColor(Theme.keys_avatar_background[AvatarDrawable.getColorIndex(colorId)]);
                color2 = getThemedColor(Theme.keys_avatar_background2[AvatarDrawable.getColorIndex(colorId)]);
            }
            avatarDrawable.setColor(color1, color2);
            invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            emoji.attach();
            imageReceiver.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            emoji.detach();
            imageReceiver.onDetachedFromWindow();
        }

        private int getThemedColor(int key) {
            return Theme.getColor(key, resourcesProvider);
        }

        private int lastColorId = -1;
        public void setColor(int colorId, boolean animated) {
            MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
            MessagesController.PeerColor peerColor = peerColors == null ? null : peerColors.getColor(lastColorId = colorId);
            final boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
            if (peerColor != null) {
                emoji.setColor(adaptProfileEmojiColor(peerColor.getBgColor1(isDark)));
                statusEmoji.setColor(ColorUtils.blendARGB(peerColor.getColor(1, resourcesProvider), peerColor.hasColor6(isDark) ? peerColor.getColor(4, resourcesProvider) : peerColor.getColor(2, resourcesProvider), .5f));
                final int accentColor = ColorUtils.blendARGB(peerColor.getStoryColor1(isDark), peerColor.getStoryColor2(isDark), .5f);
                if (!Theme.hasHue(getThemedColor(Theme.key_actionBarDefault))) {
                    subtitleView.setTextColor(accentColor);
                } else {
                    subtitleView.setTextColor(Theme.changeColorAccent(getThemedColor(Theme.key_actionBarDefault), accentColor, getThemedColor(Theme.key_avatar_subtitleInProfileBlue), isDark, accentColor));
                }
                titleView.setTextColor(Color.WHITE);
            } else {
                if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) > .8f) {
                    emoji.setColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
                } else if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) < .2f) {
                    emoji.setColor(Theme.multAlpha(getThemedColor(Theme.key_actionBarDefaultTitle), .5f));
                } else {
                    emoji.setColor(adaptProfileEmojiColor(getThemedColor(Theme.key_actionBarDefault)));
                }
                statusEmoji.setColor(Theme.getColor(Theme.key_profile_verifiedBackground, resourcesProvider));
                subtitleView.setTextColor(getThemedColor(Theme.key_actionBarDefaultSubtitle));
                titleView.setTextColor(getThemedColor(Theme.key_actionBarDefaultTitle));
            }

            storyGradient.setColorId(colorId, animated);
            invalidate();
        }

        public void setEmoji(long docId, boolean animated) {
            if (docId == 0) {
                emoji.set((Drawable) null, animated);
            } else {
                emoji.set(docId, animated);
            }
            MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
            MessagesController.PeerColor peerColor = peerColors == null ? null : peerColors.getColor(lastColorId);
            final boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
            if (peerColor != null) {
                emoji.setColor(adaptProfileEmojiColor(peerColor.getBgColor1(isDark)));
            } else if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) > .8f) {
                emoji.setColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
            } else if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) < .2f) {
                emoji.setColor(Theme.multAlpha(Theme.getColor(Theme.key_actionBarDefaultTitle), .5f));
            } else {
                emoji.setColor(adaptProfileEmojiColor(Theme.getColor(Theme.key_actionBarDefault)));
            }
            if (peerColor != null) {
                statusEmoji.setColor(ColorUtils.blendARGB(peerColor.getColor(1, resourcesProvider), peerColor.hasColor6(isDark) ? peerColor.getColor(4, resourcesProvider) : peerColor.getColor(2, resourcesProvider), .5f));
            } else {
                statusEmoji.setColor(Theme.getColor(Theme.key_profile_verifiedBackground, resourcesProvider));
            }
        }

        private final RectF rectF = new RectF();
        @Override
        protected void dispatchDraw(Canvas canvas) {
            rectF.set(dp(20.33f), getHeight() - dp(25.33f + 53.33f), dp(20.33f) + dp(53.33f), getHeight() - dp(25.33f));
            imageReceiver.setImageCoords(rectF);
            imageReceiver.draw(canvas);

            canvas.drawCircle(rectF.centerX(), rectF.centerY(), rectF.width() / 2f + dp(4), storyGradient.getPaint(rectF));

            drawProfileIconPattern(getWidth() - dp(46), getHeight(), 1f, (x, y, sz, alpha) -> {
                emoji.setAlpha((int) (0xFF * alpha));
                emoji.setBounds((int) (x - sz * .45f), (int) (y - sz * .45f), (int) (x + sz * .45f), (int) (y + sz * .45f));
                emoji.draw(canvas);
            });

            super.dispatchDraw(canvas);
        }
    }

    private int adaptProfileEmojiColor(int color) {
        final boolean isDark = AndroidUtilities.computePerceivedBrightness(color) < .2f;
        return Theme.adaptHSV(color, +.5f, isDark ? +.28f : -.28f);
    }

    private final float[] particles = {
        -18, -24.66f, 24, .4f,
        5.33f, -53, 28, .38f,
        -4, -86, 19, .18f,
        31, -30, 21, .35f,
        12, -3, 24, .18f,
        30, -73, 19, .3f,
        43, -101, 16, .1f,
        -50, 1.33f, 20, .22f,
        -58, -33, 24, .22f,
        -35, -62, 25, .22f,
        -59, -88, 19, .18f,
        -86, -61, 19, .1f,
        -90, -14.33f, 19.66f, .18f
    };
    private void drawProfileIconPattern(float cx, float cy, float scale, Utilities.Callback4<Float, Float, Float, Float> draw) {
        for (int i = 0; i < particles.length; i += 4) {
            draw.run(
                cx + dp(particles[i]) * scale,
                cy + dp(particles[i + 1]) * scale,
                dpf2(particles[i + 2]),
                particles[i + 3]
            );
        }
    }

    @Override
    public boolean isLightStatusBar() {
        if (colorBar == null) {
            return super.isLightStatusBar();
        }
        return ColorUtils.calculateLuminance(colorBar.getColor()) > 0.7f;
    }

    public void updateLightStatusBar() {
        if (getParentActivity() == null) return;
        AndroidUtilities.setLightStatusBar(getParentActivity().getWindow(), isLightStatusBar());
    }

    @Override
    public boolean isActionBarCrossfadeEnabled() {
        return false;
    }
}