package com.victu.foodatory.gallery;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor;
import com.victu.foodatory.R;

import java.util.HashSet;

public class TestActivity extends AppCompatActivity
{
    private DragSelectionProcessor.Mode mMode = DragSelectionProcessor.Mode.Simple;

    private Toolbar mToolbar;
    private DragSelectTouchListener mDragSelectTouchListener;
    private TestAutoDataAdapter mAdapter;

    private DragSelectionProcessor mDragSelectionProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle("갤러리");
        setSupportActionBar(mToolbar);


        // 1) Prepare the RecyclerView (init LayoutManager and set Adapter)
        RecyclerView rvData =  findViewById(R.id.rvData);
        GridLayoutManager glm = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        rvData.setLayoutManager(glm);
        mAdapter = new TestAutoDataAdapter(this, 500);
        rvData.setAdapter(mAdapter);
        mAdapter.setClickListener(new TestAutoDataAdapter.ItemClickListener()
        {
            @Override
            public void onItemClick(View view, int position)
            {
                mAdapter.toggleSelection(position);
            }

            @Override
            public boolean onItemLongClick(View view, int position)
            {
                // if one item is long pressed, we start the drag selection like following:
                // we just call this function and pass in the position of the first selected item
                // the selection processor does take care to update the positions selection mode correctly
                // and will correctly transform the touch events so that they can be directly applied to your mAdapter!!!
                mDragSelectTouchListener.startDragSelection(position);
                return true;
            }
        });

        // 2) Add the DragSelectListener
        mDragSelectionProcessor = new DragSelectionProcessor(new DragSelectionProcessor.ISelectionHandler() {
            @Override
            public HashSet<Integer> getSelection() {
                return mAdapter.getSelection();
            }

            @Override
            public boolean isSelected(int index) {
                return mAdapter.getSelection().contains(index);
            }

            @Override
            public void updateSelection(int start, int end, boolean isSelected, boolean calledFromOnStart) {
                mAdapter.selectRange(start, end, isSelected);
            }
        })
                .withMode(mMode);
        mDragSelectTouchListener = new DragSelectTouchListener()
                .withSelectListener(mDragSelectionProcessor);
        updateSelectionListener();
        rvData.addOnItemTouchListener(mDragSelectTouchListener);
    }

    // ---------------------
    // Selection Listener
    // ---------------------

    private void updateSelectionListener()
    {
        mDragSelectionProcessor.withMode(mMode);
    }

    // ---------------------
    // Menu
    // ---------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.menu_clear)
            mAdapter.deselectAll();
        if (item.getItemId() == R.id.menu_select_all)
            mAdapter.selectAll();

        return true;
    }
}