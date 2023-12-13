package com.example.final_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.util.Log;
import java.util.function.Consumer;
import android.util.Log;
import android.widget.TextView;

import java.util.function.Consumer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PlaceAutocompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {
    private PlacesClient placesClient;
    private List<AutocompletePrediction> predictionList = new ArrayList<>();
    private static final String TAG = "PlaceAutocompleteAdapter";


    PlaceAutocompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_expandable_list_item_2, android.R.id.text1);
        this.placesClient = placesClient;
    }

    @Override
    public int getCount() {
        return predictionList.size();
    }

    @Override
    public AutocompletePrediction getItem(int position) {
        return predictionList.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                if (charSequence != null) {
                    // Asynchronously fetch predictions
                    getAutocompleteAsync(charSequence, predictions -> {
                        filterResults.values = predictions;
                        filterResults.count = predictions.size();
                        predictionList = predictions;
                        publishResults(charSequence, filterResults);
                    });
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                if (filterResults != null && filterResults.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // If view not created, inflate your custom layout
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.autocomplete_list_item, parent, false);
        }

        // Get the text views for primary and secondary texts
        TextView textView1 = convertView.findViewById(android.R.id.text1);
        TextView textView2 = convertView.findViewById(android.R.id.text2);

        // Get the prediction item at the current position
        AutocompletePrediction item = getItem(position);

        // Set the primary and secondary text of the item to the text views
        textView1.setText(item.getPrimaryText(null).toString());
        textView2.setText(item.getSecondaryText(null).toString());

        return convertView;
    }


    private void getAutocompleteAsync(CharSequence query, Consumer<List<AutocompletePrediction>> onResult) {
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query.toString())
                // Optional: Add location bias, type filter, etc.
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            onResult.accept(response.getAutocompletePredictions());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching autocomplete predictions: " + e.getMessage());
            onResult.accept(Collections.emptyList());
        });
    }
}


