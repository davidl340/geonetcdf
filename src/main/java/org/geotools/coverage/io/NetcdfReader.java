/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2006, GeoTools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.coverage.io;

// J2SE dependencies
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.imageio.ImageReader;
import javax.media.jai.PlanarImage;

// OpenGIS dependencies
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

// Geotools dependencies
//import org.geotools.coverage.grid.GeneralGridRange;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.data.DataSourceException;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.image.io.netcdf.NetcdfImageReader;


/**
 * A reader of NetCDF files, to obtain a Grid Coverage from these files.
 *
 * @version $Id: NetcdfReader.java 25734 2007-06-04 17:57:06Z desruisseaux $
 * @author C�dric Brian�on
 */
public class NetcdfReader extends AbstractGridCoverage2DReader implements GridCoverageReader {
    /**
     * The entry to log messages during the process.
     */
    private static final Logger LOGGER = Logger.getLogger(NetcdfReader.class.toString());

    /**
     * The reader Spi for netCDF images.
     */
    private NetcdfImageReader.Spi readerSpi; 

    /**
     * The format that created this reader.
     */
    private final Format format;

    /**
     * A temporary variable to store the depth.
     * @todo modify geoserver wcs to handle 3d.
     */
    private final int depth;

    /**
     * Constructs a reader for a netCDF file.
     *
     * @param format The default netcdf format.
     * @param input The netcdf file or url for this file.
     * @param hints Null in this implementation.
     * @throws DataSourceException
     */
    public NetcdfReader(final Format format, Object input, Hints hints, int depth) throws DataSourceException {
        this.depth = depth;
        this.hints = hints;
        this.format = format;
        try {
            this.crs = CRS.decode("EPSG:4326");
        } catch (NoSuchAuthorityCodeException ex) {
            this.crs = DefaultGeographicCRS.WGS84;
        } catch (FactoryException ex) {
            this.crs = DefaultGeographicCRS.WGS84;
        }               
        this.originalEnvelope = new GeneralEnvelope(crs);
        this.originalEnvelope.setRange(0, -180, +180);
        this.originalEnvelope.setRange(1, -90, +90);
        final Rectangle actualDim = new Rectangle(0, 0, 180, 90);
        this.originalGridRange = new GridEnvelope2D(actualDim);
//        this.originalGridRange = new GridEnvelopeGeneralGridRange(originalEnvelope);
        if (input == null) {
            throw new DataSourceException("No source set to read this coverage.");
        }
        // sets the input
        source = input;
        if (source instanceof File) {
            this.coverageName = ((File)source).getName();
        } else {
            if (source instanceof URL) {
                File tmp = new File((String)source);
                this.coverageName = tmp.getName();
            } else {
                this.coverageName = "netcdf_coverage";
            }
        }
        // gets the coverage name without the extension and the dot
        final int dotIndex = coverageName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex != coverageName.length()) {
            coverageName = coverageName.substring(0, dotIndex);
        }       
    }

    /**
     * Gets information about the netCDF format.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Get the names of metadata. Not implemented in this project.
     */
    public String[] getMetadataNames() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Get the metadata value for a specified fields. Not implemented in this project.
     */
    public String getMetadataValue(String string) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Not implemented.
     */
    public String[] listSubNames() {
        return null;
    }

    /**
     * Not implemented.
     */
    public String getCurrentSubname() {
        return null;
    }

    /**
     * Not implemented.
     */
    public boolean hasMoreGridCoverages() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Read the coverage and generate the Grid Coverage associated.
     *
     * @param params Contains the parameters values for this coverage.
     * @return The grid coverage generated from the reading of the netcdf file.
     * @throws IllegalArgumentException
     * @throws IOException
     */
    @Override
    public GridCoverage2D read(GeneralParameterValue[] params) throws IllegalArgumentException, IOException {
        final GeneralEnvelope requestedEnvelope = new GeneralEnvelope(originalEnvelope);
        try {
            // Experimental; will be replaced by something more generic soon (work in progress)
            readerSpi = new NetcdfImageReader.Spi();
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
        final ImageReader reader = readerSpi.createReaderInstance(null);
        reader.setInput(source);
        RenderedImage image = reader.read(0);
        //image = new ImageWorker(image).forceComponentColorModel().getRenderedImage();
        return createImageCoverage(PlanarImage.wrapRenderedImage(image), raster2Model);
    }

    /**
     * Not implemented.
     */
    public void skip() {
        throw new UnsupportedOperationException("Only one NetCDF image supported.");
    }

    /**
     * Desallocate the input stream. If in IOException is caught, this implementation will retry.
     */
    public void dispose() {
        while (inStream != null) {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {}
            }
        }
    }
}
