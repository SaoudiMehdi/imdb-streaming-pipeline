package api;

import api.actor.FilmographyClient;
import api.actor.ActorKnownForClient;
import api.actor.PopularCelebsClient;
import api.movie.MovieMetadataClient;
import api.movie.TopRatedMoviesClient;

/**
 * Utility runner for seeding static data into CSV files.
 * Uncomment whichever step you need to run.
 */
public class DataSeedRunner {

    public static void main(String[] args) {

        // TopRatedMoviesClient topRatedMovies = new TopRatedMoviesClient();
        // topRatedMovies.saveTopRatedMovies();

        // new PopularCelebsClient().savePopularCelebs();

        // FilmographyClient.saveAllFilmography();

        // MovieMetadataClient.saveAllMovies();

        // ActorKnownForClient.saveAllKnownFor();

        // MovieMetadataClient.deleteMoviesNotInList();

        // MovieMetadataClient.deleteCelebsWithoutMovie();
    }
}