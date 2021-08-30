package com.tyron.code.ui.file.tree;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tyron.code.ui.file.tree.binder.TreeBinder;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.code.ui.main.MainFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewAdapter;

public class TreeFileManagerFragment extends Fragment {

    public static TreeFileManagerFragment newInstance(File root) {
        TreeFileManagerFragment fragment = new TreeFileManagerFragment();
        Bundle args = new Bundle();
        args.putSerializable("rootFile", root);
        fragment.setArguments(args);
        return fragment;
    }

    private File mRootFile;

    private RecyclerView mListView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRootFile = (File) getArguments().getSerializable("rootFile");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());

        mListView = new RecyclerView(requireContext());
        root.addView(mListView, new FrameLayout.LayoutParams(-1, -1));

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LinearLayoutManager manager = new LinearLayoutManager(requireContext());
        mListView.setLayoutManager(manager);

        TreeViewAdapter adapter = new TreeViewAdapter(new ArrayList<>(getNodes()), Collections.singletonList(new TreeBinder()));
        adapter.setOnTreeNodeListener(new TreeViewAdapter.OnTreeNodeListener() {
            @Override
            public boolean onClick(TreeNode treeNode, RecyclerView.ViewHolder viewHolder) {
                if (!treeNode.isLeaf()) {
                    onToggle(!treeNode.isExpand(), viewHolder);
                } else {
                    openFile(((TreeFile) treeNode.getContent()).getFile());
                    return true;
                }
                return false;
            }

            @Override
            public void onToggle(boolean isExpand, RecyclerView.ViewHolder viewHolder) {
                TreeBinder.ViewHolder holder = (TreeBinder.ViewHolder) viewHolder;
                int rotateDegree = isExpand ? 90 : -90;
                holder.arrow.animate()
                        .setDuration(180L)
                        .rotationBy(rotateDegree)
                        .start();
            }
        });
        mListView.setAdapter(adapter);

    }

    private List<TreeNode<TreeFile>> getNodes() {
        List<TreeNode<TreeFile>> nodes = new ArrayList<>();

        TreeNode<TreeFile> root = new TreeNode<>(TreeFile.fromFile(mRootFile));
        File[] childs = mRootFile.listFiles();
        if (childs != null) {
            for (File file : childs) {
                addNode(root, file);
            }
        }
        nodes.add(root);
        return nodes;
    }

    private void addNode(TreeNode<TreeFile> node, File file) {
        TreeNode<TreeFile> childNode = new TreeNode<>(TreeFile.fromFile(file));

        if (file.isDirectory()) {
            File[] childs = file.listFiles();
            if (childs != null) {
                for (File child : childs) {
                    addNode(childNode, child);
                }
            }
        }

        node.addChild(childNode);
    }

    private void openFile(File file) {
        Fragment parent = getParentFragment();

        if (parent != null) {
            if (parent instanceof MainFragment) {
                ((MainFragment) parent).openFile(file);
            }
        }
    }
}
