package com.gdavidpb.test.ui.fragments

import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.gdavidpb.test.R
import com.gdavidpb.test.domain.model.Album
import com.gdavidpb.test.domain.usecase.coroutines.Result
import com.gdavidpb.test.presentation.viewmodel.ArtistViewModel
import com.gdavidpb.test.ui.adapters.AlbumAdapter
import com.gdavidpb.test.utils.extensions.isNetworkAvailable
import com.gdavidpb.test.utils.extensions.observe
import com.gdavidpb.test.utils.extensions.toast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_artist.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArtistFragment : NavigationFragment() {

    private val viewModel: ArtistViewModel by viewModel()

    private val picasso: Picasso by inject()

    private val connectionManager: ConnectivityManager by inject()

    private val albumAdapter = AlbumAdapter(manager = AlbumManager())

    private val args by navArgs<ArtistFragmentArgs>()

    override fun onCreateView() = R.layout.fragment_artist

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(rViewAlbums) {
            layoutManager = LinearLayoutManager(context)
            adapter = albumAdapter
            setHasFixedSize(true)
        }

        with(viewModel) {
            observe(albums, ::albumsObserver)

            lookupAlbums(artistId = args.artistId)

            sRefreshAlbums.setOnRefreshListener {
                lookupAlbums(artistId = args.artistId)
            }
        }
    }

    private fun albumsObserver(result: Result<List<Album>>?) {
        when (result) {
            is Result.OnLoading -> {
                sRefreshAlbums.isRefreshing = true
            }
            is Result.OnSuccess -> {
                sRefreshAlbums.isRefreshing = false

                val albums = result.value
                    .sortedByDescending { it.releaseDate }

                if (albums.isNotEmpty()) {
                    tViewAlbums.visibility = View.GONE
                    rViewAlbums.visibility = View.VISIBLE
                } else {
                    rViewAlbums.visibility = View.GONE
                    tViewAlbums.visibility = View.VISIBLE
                }

                albumAdapter.swapItems(new = albums)
            }
            is Result.OnError -> {
                sRefreshAlbums.isRefreshing = false

                val messageResource = if (connectionManager.isNetworkAvailable())
                    R.string.toast_connection_failure
                else
                    R.string.toast_no_connection

                requireContext().toast(messageResource)
            }
            else -> {
                sRefreshAlbums.isRefreshing = false

                requireContext().toast(R.string.toast_unexpected_failure)
            }
        }
    }

    inner class AlbumManager : AlbumAdapter.AdapterManager {
        override fun onAlbumClicked(item: Album, position: Int) {
            val destination = ArtistFragmentDirections.navToAlbum(
                albumId = item.collectionId,
                albumName = item.collectionName
            )

            navigate(destination)
        }

        override fun provideImageLoader(): Picasso {
            return picasso
        }
    }
}