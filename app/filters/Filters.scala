package filters

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
/**
 * Created by Oudam on 10/18/2016.
 */
class Filters @Inject() (corsFilter: CORSFilter)
  extends DefaultHttpFilters(corsFilter)
