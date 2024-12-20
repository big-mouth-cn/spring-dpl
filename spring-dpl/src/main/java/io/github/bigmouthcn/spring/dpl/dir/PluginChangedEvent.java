package io.github.bigmouthcn.spring.dpl.dir;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.ToString;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * 插件变更事件
 *
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@ToString
public class PluginChangedEvent extends EventObject {

    private static final long serialVersionUID = 1790843367218320922L;

    private final List<String> added = Lists.newArrayList();
    private final List<String> updated = Lists.newArrayList();
    private final List<String> removed = Lists.newArrayList();
    private final List<String> all = Lists.newArrayList();

    public PluginChangedEvent(Object source, Snapshot old, Snapshot currently) {
        super(source);
        Preconditions.checkNotNull(currently);
        this.init(old, currently);
    }

    private void init(Snapshot old, Snapshot currently) {
        if (null == old) {
            this.added.addAll(currently.getFiles().keySet());
        } else {
            Map<String, Long> oldFiles = old.getFiles();
            Map<String, Long> currentlyFiles = currently.getFiles();
            for (Map.Entry<String, Long> e : oldFiles.entrySet()) {
                String filename = e.getKey();
                long modifyTime = e.getValue();
                if (!currentlyFiles.containsKey(filename)) {
                    removed.add(filename);
                } else {
                    if (modifyTime != currentlyFiles.get(filename)) {
                        updated.add(filename);
                    }
                }
            }

            for (Map.Entry<String, Long> e : currentlyFiles.entrySet()) {
                String filename = e.getKey();
                if (!oldFiles.containsKey(filename)) {
                    added.add(filename);
                }
            }
        }
        all.addAll(currently.getFiles().keySet());
    }

    public List<String> getAdded() {
        return added;
    }

    public List<String> getUpdated() {
        return updated;
    }

    public List<String> getRemoved() {
        return removed;
    }

    public List<String> getAll() {
        return all;
    }
}
