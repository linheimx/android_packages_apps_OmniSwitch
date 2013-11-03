/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omniswitch;

import java.util.ArrayList;
import java.util.List;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.RemoteException;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class RecentsLayout extends LinearLayout {
	private static final String TAG = "RecentsLayout";
	private WindowManager mWindowManager;
	private LayoutInflater mInflater;
	private ListView mRecentList;
	private HorizontalListView mRecentListHorizontal;
	private ImageButton mLastAppButton;
	private ImageButton mKillAllButton;
	private ImageButton mKillOtherButton;
	private ImageButton mHomeButton;
	private ImageButton mSettingsButton;
	private RecentListAdapter mRecentListAdapter;
	private List<TaskDescription> mLoadedTasks;
	private Context mContext;
	private RecentsManager mRecentsManager;
	private FrameLayout mPopupView;
	private boolean mShowing;
	private PopupMenu mPopup;
	private int mBackgroundColor = Color.BLACK;
	private int mBackgroundOpacity = 60;
	private boolean mHorizontal;
	private View mView;
	private int mLocation = 0; // 0 = right 1 = left 
	private boolean mAnimate = true;

	public class RecentListAdapter extends ArrayAdapter<TaskDescription> {

		public RecentListAdapter(Context context, int resource,
				List<TaskDescription> values) {
			super(context, R.layout.recent_item, resource, values);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = null;
			TaskDescription ad = mLoadedTasks.get(position);

			if (mHorizontal) {
				rowView = mInflater.inflate(R.layout.recent_item_horizontal, parent, false);
				final TextView item = (TextView) rowView
						.findViewById(R.id.recent_item);
				item.setText(ad.getLabel());
				item.setCompoundDrawablesWithIntrinsicBounds(null, ad.getIcon() , null, null);
			} else {
				rowView = mInflater.inflate(R.layout.recent_item, parent, false);			
				final TextView item = (TextView) rowView
						.findViewById(R.id.recent_item);
				item.setText(ad.getLabel());
				item.setCompoundDrawablesWithIntrinsicBounds(ad.getIcon(), null , null, null);
			}
			return rowView;
		}
	}

	public RecentsLayout(Context context, AttributeSet attrs,
			RecentsManager manager) {
		super(context, attrs);
		mContext = context;
		mRecentsManager = manager;
		mWindowManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		mInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLoadedTasks = new ArrayList<TaskDescription>();
		mRecentListAdapter = new RecentListAdapter(mContext,
				android.R.layout.simple_list_item_multiple_choice, mLoadedTasks);		
	}

	private void createView() {
		mView = null;
		if (mHorizontal) {
			mView = mInflater.inflate(R.layout.recents_list_horizontal, this, false);
			mRecentListHorizontal = (HorizontalListView)mView.findViewById(R.id.recent_list_horizontal);

			mRecentListHorizontal.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					Log.d(TAG, "onItemClick");
					TaskDescription task = mLoadedTasks.get(position);
					mRecentsManager.switchTask(task);
					hide();
				}
			});

			mRecentListHorizontal
					.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> parent,
								View view, int position, long id) {
							Log.d(TAG, "onItemLongClick");
							TaskDescription task = mLoadedTasks.get(position);
							handleLongPress(task, view);
							return true;
						}
					});

			SwipeDismissHorizontalListViewTouchListener touchListener = new SwipeDismissHorizontalListViewTouchListener(
					mRecentListHorizontal,
					new SwipeDismissHorizontalListViewTouchListener.DismissCallbacks() {
						public void onDismiss(HorizontalListView listView,
								int[] reverseSortedPositions) {
							for (int position : reverseSortedPositions) {
								TaskDescription ad = mRecentListAdapter
										.getItem(position);
								mRecentsManager.killTask(ad);
								break;
							}
						}

						@Override
						public boolean canDismiss(int position) {
							return true;
						}
					});

			mRecentListHorizontal.setSwipeListener(touchListener);

			mRecentListHorizontal.setAdapter(mRecentListAdapter);		
		} else {
			mView = mInflater.inflate(R.layout.recents_list, this, false);
			mRecentList = (ListView) mView.findViewById(R.id.recent_list);

			mRecentList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					Log.d(TAG, "onItemClick");
					TaskDescription task = mLoadedTasks.get(position);
					mRecentsManager.switchTask(task);
					hide();
				}
			});

			mRecentList
					.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> parent,
								View view, int position, long id) {
							Log.d(TAG, "onItemLongClick");
							TaskDescription task = mLoadedTasks.get(position);
							handleLongPress(task, view);
							return true;
						}
					});

			SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(
					mRecentList,
					new SwipeDismissListViewTouchListener.DismissCallbacks() {
						public void onDismiss(ListView listView,
								int[] reverseSortedPositions) {
							for (int position : reverseSortedPositions) {
								TaskDescription ad = mRecentListAdapter
										.getItem(position);
								mRecentsManager.killTask(ad);
								break;
							}
						}

						@Override
						public boolean canDismiss(int position) {
							return true;
						}
					});
			mRecentList.setOnTouchListener(touchListener);
			mRecentList.setOnScrollListener(touchListener.makeScrollListener());

			mRecentList.setAdapter(mRecentListAdapter);
		}

		mHomeButton= (ImageButton) mView.findViewById(R.id.home);
		mHomeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mRecentsManager.dismissAndGoHome();
			}
		});

		mLastAppButton = (ImageButton) mView.findViewById(R.id.lastApp);
		mLastAppButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mRecentsManager.toggleLastApp();
			}
		});

		mKillAllButton = (ImageButton) mView.findViewById(R.id.killAll);
		mKillAllButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mRecentsManager.killAll();
			}
		});

		mKillOtherButton = (ImageButton) mView.findViewById(R.id.killOther);
		mKillOtherButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mRecentsManager.killOther();
			}
		});

		mSettingsButton = (ImageButton) mView.findViewById(R.id.settings);
		mSettingsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent hideRecent = new Intent(
						RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
				mContext.sendBroadcast(hideRecent);

				Intent mainActivity = new Intent(mContext,
						SettingsActivity.class);
				mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
				mContext.startActivity(mainActivity);
			}
		});
		
		mPopupView = new FrameLayout(mContext);
		mPopupView.setBackgroundColor(mBackgroundColor);
		mPopupView.getBackground().setAlpha(mBackgroundOpacity);

		mPopupView.removeAllViews();
		
		mPopupView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "onTouch");
				Intent hideRecent = new Intent(
						RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
				mContext.sendBroadcast(hideRecent);
				return true;
			}
		});
		mPopupView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Log.d(TAG, "onKey");
				Intent hideRecent = new Intent(
						RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
				mContext.sendBroadcast(hideRecent);
				return true;
			}
		});
	}

	public void show() {
		if (mShowing) {
			return;
		}

		createView();
		
		mWindowManager.addView(mPopupView, getParams());

		mPopupView.addView(mView);
		
		if (mAnimate){
			mView.startAnimation(getShowAnimation());
		} else {
			mPopupView.setFocusableInTouchMode(true);
			mShowing = true;
		}
	}

	private Animation getShowAnimation() {
		int animId = R.anim.slide_right_in;
		
		if (mLocation == 1){
			animId = R.anim.slide_left_in;
		}
		Animation animation = AnimationUtils.loadAnimation(mContext, animId);
		animation.setStartOffset(0);
		animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
        		mPopupView.setFocusableInTouchMode(true);
        		mShowing = true;
            }

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});
		return animation;
	}
	
	private Animation getHideAnimation() {
		int animId = R.anim.slide_right_out;
		
		if (mLocation == 1){
			animId = R.anim.slide_left_out;
		}

		Animation animation = AnimationUtils.loadAnimation(mContext, animId);
		animation.setStartOffset(0);
		animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
        		mWindowManager.removeView(mPopupView);
        		mPopupView = null;
        		mShowing = false;            	
            }

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});
		return animation;
	}
	
	public void hide() {
		if (!mShowing) {
			return;
		}
		if (mPopup != null) {
			mPopup.dismiss();
		}

		if (mAnimate){
			mView.startAnimation(getHideAnimation());
		} else {
    		mWindowManager.removeView(mPopupView);
    		mPopupView = null;
    		mShowing = false;			
		}
	}

	private WindowManager.LayoutParams getParams() {
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				mHorizontal?WindowManager.LayoutParams.MATCH_PARENT:WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		params.gravity = getGravity();
		return params;
	}

	private int getGravity() {
		return Gravity.CENTER;
	}
	
	public void update(List<TaskDescription> taskList) {
		Log.d(TAG, "update");
		mLoadedTasks.clear();
		mLoadedTasks.addAll(taskList);
		mRecentListAdapter.notifyDataSetChanged();
	}

	private void handleLongPress(final TaskDescription ad, View view) {
		final PopupMenu popup = new PopupMenu(mContext, view);
		mPopup = popup;
		popup.getMenuInflater().inflate(R.menu.recent_popup_menu,
				popup.getMenu());
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.recent_remove_item) {
					mRecentsManager.killTask(ad);
				} else if (item.getItemId() == R.id.recent_inspect_item) {
					startApplicationDetailsActivity(ad.getPackageName());
				} else {
					return false;
				}
				return true;
			}
		});
		popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
			public void onDismiss(PopupMenu menu) {
				mPopup = null;
			}
		});
		popup.show();
	}

	private void startApplicationDetailsActivity(String packageName) {
		Intent hideRecent = new Intent(
				RecentsService.RecentsReceiver.ACTION_HIDE_RECENTS);
		mContext.sendBroadcast(hideRecent);

		Intent intent = new Intent(
				Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
						"package", packageName, null));
		intent.setComponent(intent.resolveActivity(mContext.getPackageManager()));
		TaskStackBuilder.create(getContext())
				.addNextIntentWithParentStack(intent).startActivities();
	}

	public void handleSwipe(TaskDescription ad) {
		mRecentsManager.killTask(ad);
	}
	
	public void updatePrefs(SharedPreferences prefs, String key){
		Log.d(TAG, "updatePrefs");
		mHorizontal = prefs.getString(SettingsActivity.PREF_ORIENTATION, "vertical").equals("horizontal");		
		String location = prefs.getString(SettingsActivity.PREF_DRAG_HANDLE_LOCATION, "0");
		mLocation = Integer.valueOf(location);
		int opacity = prefs.getInt(SettingsActivity.PREF_OPACITY, 60);
		mBackgroundOpacity = (int)(255 * ((float)opacity/100.0f));
		mAnimate = prefs.getBoolean(SettingsActivity.PREF_ANIMATE, true);
	}
}
