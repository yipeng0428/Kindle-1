package org.xdty.kindle.module.database;

import org.xdty.kindle.application.Application;
import org.xdty.kindle.module.Book;
import org.xdty.kindle.module.Node;
import org.xdty.kindle.module.NodeMap;
import org.xdty.kindle.module.NodeRelation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

public class DatabaseImpl implements Database {

    private final static int PAGE_SIZE = 50;
    @Inject
    EntityDataStore<Persistable> mDataStore;

    private int mCurrentPage = 0;

    private DatabaseImpl() {
        Application.getAppComponent().inject(this);
    }

    public static DatabaseImpl getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @Override
    public List<Book> getCnBooksSync() {
        return mDataStore.select(Book.class)
                .where(Book.LANGUAGES.eq("chinese"))
                .or(Book.LANGUAGES.eq("traditional_chinese"))
                .limit(PAGE_SIZE)
                .offset(mCurrentPage)
                .get()
                .toList();
    }

    @Override
    public List<Book> getEnBooksSync() {
        return mDataStore.select(Book.class)
                .where(Book.LANGUAGES.ne("chinese"))
                .and(Book.LANGUAGES.ne("traditional_chinese"))
                .limit(PAGE_SIZE)
                .offset(mCurrentPage)
                .get()
                .toList();
    }

    @Override
    public List<Node> getBookNodesSync(String itemId) {

        return getBookNodesSync(itemId, true);
    }

    @Override
    public Node getNodeParentSync(long nodeId) {
        return mDataStore.select(Node.class)
                .where(Node.NODE_ID.in(mDataStore.select(NodeRelation.ANCESTOR)
                        .where(NodeRelation.DESCENDANT.eq(nodeId)))).get().firstOrNull();
    }

    private List<Node> getBookNodesSync(String itemId, boolean withParent) {

        List<NodeMap> list = mDataStore.select(NodeMap.class)
                .where(NodeMap.ITEM_ID.in(Arrays.asList(itemId)))
                .get()
                .toList();

        mDataStore.transaction();

        List<Long> l = new ArrayList<>();

        for (NodeMap n : list) {
            l.add(n.getNodeId());
        }

        // fixme: empty result
        List<Node> nodes = mDataStore.select(Node.class)
                .where(Node.NODE_ID.in(l))
                .get()
                .toList();

        if (withParent) {
            for (Node node : nodes) {
                Node n;
                do {
                    n = getNodeParentSync(node.getNodeId());
                    node.setNode(n);
                    node = n;
                } while (n != null);
            }
        }

        return nodes;
    }

    private static class SingletonHelper {
        private final static DatabaseImpl INSTANCE = new DatabaseImpl();
    }

}
