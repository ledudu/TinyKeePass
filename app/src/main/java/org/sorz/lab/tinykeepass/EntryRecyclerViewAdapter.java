package org.sorz.lab.tinykeepass;

import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import de.slackspace.openkeepass.domain.KeePassFile;
import de.slackspace.openkeepass.domain.Entry;


public class EntryRecyclerViewAdapter extends RecyclerView.Adapter<EntryRecyclerViewAdapter.ViewHolder> {
    private final static String TAG = EntryRecyclerViewAdapter.class.getName();
    private final MainActivity activity;
    private final List<Entry> allEntries;
    private List<Entry> entries;


    public EntryRecyclerViewAdapter(MainActivity activity) {
        this.activity = activity;
        KeePassFile db = KeePassStorage.getKeePassFile();
        if (db != null) {
            if (db.getMeta().getRecycleBinEnabled()) {
                allEntries = new ArrayList<>();
                UUID recycleBin = db.getMeta().getRecycleBinUuid();
                db.getGroups().stream()
                        .filter(g -> !g.getUuid().equals(recycleBin))
                        .forEach(g -> allEntries.addAll(g.getEntries()));
            } else {
                allEntries = db.getEntries();
            }
            Log.d(TAG, allEntries.size() + " entries loaded");

        } else {
            Log.w(TAG, "database is locked");
            allEntries = new ArrayList<>();
        }
        entries = allEntries;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Entry entry = entries.get(position);
        holder.entry = entry;
        holder.imageIcon.setImageBitmap(
                BitmapFactory.decodeByteArray(entry.getIconData(), 0,
                        entry.getIconData().length));
        holder.textTitle.setText(parse(entry.getTitle()));
        holder.textUsername.setText(parse(entry.getUsername()));

        String url = parse(entry.getUrl()).replaceFirst("https?://(www\\.)?", "");
        String[] hostnamePath = url.split("/", 2);
        holder.textUrlHostname.setText(hostnamePath[0]);
        holder.textUrlPath.setText(hostnamePath.length > 1 ? "/" + hostnamePath[1] : "");

        holder.view.setOnClickListener(v -> {
            if (null != activity) {
                activity.copyEntry(entry);
            }
        });
    }

    private static String parse(String s) {
        return s == null ? "" : s;
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final ImageView imageIcon;
        final TextView textTitle;
        final TextView textUsername;
        final TextView textUrlHostname;
        final TextView textUrlPath;
        Entry entry;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            imageIcon = view.findViewById(R.id.imageIcon);
            textTitle = view.findViewById(R.id.textTitle);
            textUsername = view.findViewById(R.id.textUsername);
            textUrlHostname = view.findViewById(R.id.textUrlHostname);
            textUrlPath =  view.findViewById(R.id.textUrlPath);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + textTitle.getText() + "'";
        }
    }

    private static boolean contains(String string, String query) {
        return string != null && string.toLowerCase().contains(query);
    }

    public void setFilter(String query) {
        if (query == null || query.isEmpty()) {
            entries = allEntries;
        } else {
            final String q = query.toLowerCase().trim();
            entries = allEntries.parallelStream().filter(e ->
                    contains(e.getTitle(), q) ||
                    contains(e.getUrl(), q)  ||
                    contains(e.getNotes(), q)  ||
                    contains(e.getUsername(), q)
            ).collect(Collectors.toList());
        }
        notifyDataSetChanged();
    }

}